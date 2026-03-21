import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';

class DownloadProgress {
  final int received;
  final int total;
  final double? percent; // 0.0~1.0, null이면 알 수 없음

  const DownloadProgress({
    required this.received,
    required this.total,
    this.percent,
  });

  String get receivedStr => _fmt(received);
  String get totalStr => _fmt(total);

  static String _fmt(int bytes) {
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(0)} KB';
    if (bytes < 1024 * 1024 * 1024) {
      return '${(bytes / (1024 * 1024)).toStringAsFixed(0)} MB';
    }
    return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(2)} GB';
  }
}

class ModelManager {
  /// Llama 3.2 1B Instruct Q4_K_M
  /// - 크기: ~800MB
  /// - 성능: 모바일에서 빠른 추론 가능
  /// - 한국어: 기본 지원
  static const modelUrl =
      'https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF'
      '/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf';

  static const _filename = 'Llama-3.2-1B-Instruct-Q4_K_M.gguf';

  /// 예상 최소 파일 크기 (다운 완료 판별용)
  static const _minValidSize = 600 * 1024 * 1024; // 600MB

  // ─── 경로 ────────────────────────────────────────────────────
  static Future<String> get modelPath async {
    final dir = await getApplicationDocumentsDirectory();
    return '${dir.path}/$_filename';
  }

  // ─── 상태 확인 ───────────────────────────────────────────────
  static Future<bool> isReady() async {
    final path = await modelPath;
    final file = File(path);
    if (!file.existsSync()) return false;
    return (await file.length()) >= _minValidSize;
  }

  /// 현재 다운로드된 바이트 수 (재시작 지점 계산용)
  static Future<int> _downloadedBytes() async {
    final path = await modelPath;
    final file = File(path);
    return file.existsSync() ? await file.length() : 0;
  }

  // ─── 다운로드 (재시작 지원) ──────────────────────────────────
  static Stream<DownloadProgress> download() async* {
    final path = await modelPath;
    final file = File(path);
    final startByte = await _downloadedBytes();

    final client = http.Client();
    try {
      final request = http.Request('GET', Uri.parse(modelUrl));

      // Range 헤더 = 이어받기
      if (startByte > 0) {
        request.headers['Range'] = 'bytes=$startByte-';
      }

      final response = await client.send(request);

      // 200 = 처음, 206 = 이어받기
      if (response.statusCode != 200 && response.statusCode != 206) {
        throw Exception('서버 오류: HTTP ${response.statusCode}');
      }

      final contentLength = response.contentLength ?? 0;
      final total = startByte + contentLength;
      var received = startByte;

      final sink = file.openWrite(
        mode: startByte > 0 ? FileMode.append : FileMode.write,
      );

      try {
        await for (final chunk in response.stream) {
          sink.add(chunk);
          received += chunk.length;
          yield DownloadProgress(
            received: received,
            total: total,
            percent: total > 0 ? (received / total).clamp(0.0, 1.0) : null,
          );
        }
      } finally {
        await sink.flush();
        await sink.close();
      }
    } finally {
      client.close();
    }
  }

  // ─── 삭제 ────────────────────────────────────────────────────
  static Future<void> delete() async {
    final path = await modelPath;
    final file = File(path);
    if (file.existsSync()) await file.delete();
  }
}
