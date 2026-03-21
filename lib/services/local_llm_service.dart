import 'dart:async';
import 'package:llama_cpp_dart/llama_cpp_dart.dart';
import '../models/character.dart';
import '../models/message.dart';

class LocalLlmService {
  LlamaProcessor? _processor;
  bool _ready = false;
  bool _generating = false;

  bool get isReady => _ready;

  // ─── 초기화 ──────────────────────────────────────────────────
  Future<void> initialize(String modelPath) async {
    _dispose();

    // llama_cpp_dart 설정
    final modelParams = LlamaModel(modelPath);

    final contextParams = ContextParams()
      ..nCtx = 2048   // 컨텍스트 윈도우 (메모리 ↔ 기억 길이 트레이드오프)
      ..nBatch = 512;

    final samplerParams = SamplerParams()
      ..temperature = 0.8
      ..topP = 0.9
      ..topK = 40;

    _processor = LlamaProcessor(modelParams, samplerParams, contextParams);
    _ready = true;
  }

  // ─── 채팅 ────────────────────────────────────────────────────
  Future<String> chat({
    required Character character,
    required List<Message> history,
    required String userMessage,
  }) async {
    if (!_ready || _processor == null) {
      throw Exception('AI가 초기화되지 않았습니다. 앱을 재시작해주세요.');
    }
    if (_generating) {
      throw Exception('이미 생성 중입니다.');
    }

    _generating = true;
    final completer = Completer<String>();
    final buffer = StringBuffer();

    _processor!.setTokenCallback((token) {
      if (completer.isCompleted) return;

      // Llama 3.2 종료 토큰
      if (_isStopToken(token)) {
        _generating = false;
        completer.complete(_clean(buffer.toString()));
        return;
      }
      buffer.write(token);
    });

    final prompt = _buildPrompt(character, history, userMessage);
    _processor!.prompt(prompt);

    try {
      return await completer.future.timeout(
        const Duration(seconds: 90),
        onTimeout: () {
          _generating = false;
          final text = _clean(buffer.toString());
          return text.isNotEmpty ? text : '...';
        },
      );
    } finally {
      _generating = false;
    }
  }

  // ─── Llama 3.2 Instruct 채팅 템플릿 ─────────────────────────
  String _buildPrompt(
    Character character,
    List<Message> history,
    String userMessage,
  ) {
    final sb = StringBuffer();

    sb.write('<|begin_of_text|>');
    sb.write('<|start_header_id|>system<|end_header_id|>\n\n');
    sb.write(character.buildSystemPrompt());
    sb.write('<|eot_id|>');

    // 최근 대화 히스토리 (최대 10개)
    for (final msg in history.takeLast(10)) {
      final role = msg.isUser ? 'user' : 'assistant';
      sb.write('<|start_header_id|>$role<|end_header_id|>\n\n');
      sb.write(msg.content);
      sb.write('<|eot_id|>');
    }

    // 현재 사용자 메시지
    sb.write('<|start_header_id|>user<|end_header_id|>\n\n');
    sb.write(userMessage);
    sb.write('<|eot_id|>');
    sb.write('<|start_header_id|>assistant<|end_header_id|>\n\n');

    return sb.toString();
  }

  bool _isStopToken(String token) {
    return token.contains('<|eot_id|>') ||
        token.contains('<|end_of_text|>') ||
        token.contains('<|end_header_id|>');
  }

  String _clean(String text) {
    return text
        .replaceAll('<|eot_id|>', '')
        .replaceAll('<|end_of_text|>', '')
        .replaceAll('<|end_header_id|>', '')
        .replaceAll('<|start_header_id|>', '')
        .trim();
  }

  void _dispose() {
    _processor?.dispose();
    _processor = null;
    _ready = false;
    _generating = false;
  }

  void dispose() => _dispose();
}

extension _ListTakeLast<T> on List<T> {
  List<T> takeLast(int n) =>
      length <= n ? this : sublist(length - n);
}
