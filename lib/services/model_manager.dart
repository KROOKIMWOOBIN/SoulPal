import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';

class DownloadProgress {
  final int received;
  final int total;
  final double? percent;

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
  static const modelUrl =
      'https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF'
      '/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf';

  static const _filename = 'Llama-3.2-1B-Instruct-Q4_K_M.gguf';
  static const _minValidSize = 700 * 1024 * 1024; // 700MB
  static const _maxRetries = 3;

  // ─── 저장 경로 ────────────────────────────────────────────────
  // 외부 앱 저장소 우선 사용 (/sdcard/Android/data/<pkg>/files/)
  // → 앱 업데이트 시 유지됨 (앱 삭제 시에만 제거)
  // 외부 저장소 사용 불가 시 내부 저장소 fallback
  static Future<String> get modelPath async {
    final dir = await _storageDir();
    return '${dir.path}/$_filename';
  }

  static Future<Directory> _storageDir() async {
    final external = await getExternalStorageDirectory();
    if (external != null) return external;
    return getApplicationDocumentsDirectory();
  }

  // ─── 상태 확인 (구 경로 자동 마이그레이션 포함) ──────────────
  static Future<bool> isReady() async {
    await _migrateIfNeeded();
    final path = await modelPath;
    final file = File(path);
    if (!file.existsSync()) return false;
    return (await file.length()) >= _minValidSize;
  }

  /// 내부 저장소에 모델이 있으면 외부 저장소로 이동 (1회)
  static Future<void> _migrateIfNeeded() async {
    final external = await getExternalStorageDirectory();
    if (external == null) return; // 외부 저장소 없으면 마이그레이션 불필요

    final newPath = '${external.path}/$_filename';
    if (File(newPath).existsSync()) return; // 이미 외부에 있음

    final internalDir = await getApplicationDocumentsDirectory();
    final oldPath = '${internalDir.path}/$_filename';
    final oldFile = File(oldPath);

    if (!oldFile.existsSync()) return;
    final size = await oldFile.length();
    if (size < _minValidSize) return; // 불완전한 파일이면 무시

    // 복사 후 구 파일 삭제
    await oldFile.copy(newPath);
    await oldFile.delete();
  }

  static Future<int> _downloadedBytes() async {
    final path = await modelPath;
    final file = File(path);
    return file.existsSync() ? await file.length() : 0;
  }

  // ─── 다운로드 (재시도 + 이어받기) ────────────────────────────
  static Stream<DownloadProgress> download() async* {
    for (int attempt = 1; attempt <= _maxRetries; attempt++) {
      try {
        yield* _downloadOnce();
        return;
      } on SocketException catch (e) {
        if (attempt == _maxRetries) {
          throw Exception('네트워크 연결 오류 (소켓): $e\n인터넷 연결을 확인해주세요.');
        }
        await Future.delayed(Duration(seconds: attempt * 2));
      } on HttpException catch (e) {
        if (attempt == _maxRetries) {
          throw Exception('HTTP 오류: $e');
        }
        await Future.delayed(Duration(seconds: attempt * 2));
      } on Exception catch (e) {
        final msg = e.toString();
        if (msg.contains('HTTP 4') || attempt == _maxRetries) rethrow;
        await Future.delayed(Duration(seconds: attempt * 2));
      }
    }
  }

  static Stream<DownloadProgress> _downloadOnce() async* {
    final path = await modelPath;
    final file = File(path);
    final startByte = await _downloadedBytes();

    final client = http.Client();
    try {
      final request = http.Request('GET', Uri.parse(modelUrl));
      request.headers['User-Agent'] = 'SoulPal/1.0 (Android)';

      if (startByte > 0) {
        request.headers['Range'] = 'bytes=$startByte-';
      }

      final response = await client.send(request).timeout(
        const Duration(seconds: 30),
        onTimeout: () => throw SocketException('연결 시간 초과'),
      );

      if (response.statusCode != 200 && response.statusCode != 206) {
        throw Exception(
          'HTTP ${response.statusCode}: '
          '${_httpErrorDescription(response.statusCode)}',
        );
      }

      final effectiveStart =
          (startByte > 0 && response.statusCode == 200) ? 0 : startByte;

      final contentLength = response.contentLength ?? 0;
      final total = effectiveStart + contentLength;
      var received = effectiveStart;

      final sink = file.openWrite(
        mode: effectiveStart > 0 ? FileMode.append : FileMode.write,
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

  static String _httpErrorDescription(int code) {
    switch (code) {
      case 401: return '인증 필요 (401)';
      case 403: return '접근 거부 (403)';
      case 404: return '파일을 찾을 수 없음 (404)';
      case 429: return '요청 횟수 초과, 잠시 후 다시 시도 (429)';
      default:  return '서버 오류 ($code)';
    }
  }

  // ─── 삭제 ────────────────────────────────────────────────────
  static Future<void> delete() async {
    final path = await modelPath;
    final file = File(path);
    if (file.existsSync()) await file.delete();
  }
}
