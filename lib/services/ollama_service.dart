import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/character.dart';
import '../models/message.dart';

class OllamaException implements Exception {
  final String message;
  OllamaException(this.message);
  @override
  String toString() => 'OllamaException: $message';
}

class PullProgress {
  final String status;
  final double? percent; // 0.0 ~ 1.0, null이면 진행률 미확인
  const PullProgress({required this.status, this.percent});
}

class OllamaService {
  static const _model = 'llama3';
  static const _timeout = Duration(seconds: 120);

  final String baseUrl;

  OllamaService({required this.baseUrl});

  Uri get _chatUri => Uri.parse('$baseUrl/api/chat');
  Uri get _tagsUri => Uri.parse('$baseUrl/api/tags');
  Uri get _pullUri => Uri.parse('$baseUrl/api/pull');

  /// Ollama 서버 실행 여부만 확인 (모델 존재 무관)
  Future<bool> isServerRunning() async {
    try {
      await http.get(_tagsUri).timeout(const Duration(seconds: 5));
      return true;
    } catch (_) {
      return false;
    }
  }

  /// llama3 모델이 로컬에 있는지 확인
  Future<bool> isModelReady() async {
    try {
      final response =
          await http.get(_tagsUri).timeout(const Duration(seconds: 5));
      if (response.statusCode != 200) return false;
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      final models = body['models'] as List? ?? [];
      return models.any((m) =>
          (m['name'] as String? ?? '').startsWith(_model));
    } catch (_) {
      return false;
    }
  }

  /// Check if Ollama is running and llama3 is available
  Future<bool> isAvailable() async => isModelReady();

  /// llama3 모델 다운로드 (스트리밍 진행률 콜백)
  /// [onProgress] : 진행 상황마다 호출됨
  /// 완료되면 정상 반환, 실패 시 OllamaException throw
  Future<void> pullModel({
    required void Function(PullProgress) onProgress,
  }) async {
    final client = http.Client();
    try {
      final request = http.Request('POST', _pullUri)
        ..headers['Content-Type'] = 'application/json'
        ..body = jsonEncode({'name': _model, 'stream': true});

      final streamedResponse = await client.send(request);

      if (streamedResponse.statusCode != 200) {
        throw OllamaException(
            'Pull failed: HTTP ${streamedResponse.statusCode}');
      }

      // 스트리밍 JSON lines 파싱
      final buffer = StringBuffer();
      await for (final chunk
          in streamedResponse.stream.transform(utf8.decoder)) {
        buffer.write(chunk);
        // JSON lines는 \n으로 구분
        final lines = buffer.toString().split('\n');
        // 마지막 라인은 불완전할 수 있으므로 버퍼에 유지
        buffer
          ..clear()
          ..write(lines.last);

        for (final line in lines.sublist(0, lines.length - 1)) {
          final trimmed = line.trim();
          if (trimmed.isEmpty) continue;
          try {
            final json = jsonDecode(trimmed) as Map<String, dynamic>;
            final status = json['status'] as String? ?? '';
            final total = (json['total'] as num?)?.toDouble();
            final completed = (json['completed'] as num?)?.toDouble();

            double? percent;
            if (total != null && total > 0 && completed != null) {
              percent = completed / total;
            }

            onProgress(PullProgress(status: status, percent: percent));

            if (status == 'success') return;
          } catch (_) {
            // 파싱 실패한 라인 무시
          }
        }
      }
    } catch (e) {
      if (e is OllamaException) rethrow;
      throw OllamaException('모델 다운로드 실패: $e');
    } finally {
      client.close();
    }
  }

  /// Send chat and get response (non-streaming)
  Future<String> chat({
    required Character character,
    required List<Message> history,
    required String userMessage,
  }) async {
    final systemPrompt = character.buildSystemPrompt();

    // Build messages list for API
    final messages = <Map<String, String>>[
      {'role': 'system', 'content': systemPrompt},
      // Include last 20 messages for context
      ...history.takeLast(20).map((m) => m.toOllamaMessage()),
      {'role': 'user', 'content': userMessage},
    ];

    final body = jsonEncode({
      'model': _model,
      'messages': messages,
      'stream': false,
      'options': {
        'temperature': 0.8,
        'top_p': 0.9,
        'num_predict': 300,
      },
    });

    try {
      final response = await http
          .post(
            _chatUri,
            headers: {'Content-Type': 'application/json'},
            body: body,
          )
          .timeout(_timeout);

      if (response.statusCode != 200) {
        throw OllamaException(
            'HTTP ${response.statusCode}: ${response.body}');
      }

      final decoded = jsonDecode(response.body) as Map<String, dynamic>;
      final message = decoded['message'] as Map<String, dynamic>?;
      final content = message?['content'] as String?;

      if (content == null || content.isEmpty) {
        throw OllamaException('Empty response from model');
      }

      return content.trim();
    } on OllamaException {
      rethrow;
    } catch (e) {
      throw OllamaException('Connection failed: $e\n\nOllama가 실행 중인지 확인해주세요.');
    }
  }
}

extension _ListTakeLast<T> on List<T> {
  List<T> takeLast(int n) =>
      length <= n ? this : sublist(length - n);
}
