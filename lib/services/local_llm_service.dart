import 'dart:async';
import 'package:llama_cpp_dart/llama_cpp_dart.dart' hide Message;
import '../models/character.dart';
import '../models/message.dart';

class LocalLlmService {
  LlamaParent? _parent;
  bool _ready = false;
  bool _generating = false;

  bool get isReady => _ready;

  // ─── 초기화 (별도 isolate에서 실행 — UI 블로킹 없음) ───────────
  Future<void> initialize(
    String modelPath, {
    double temperature = 0.8,
    int contextLength = 2048,
  }) async {
    await _dispose();

    final loadCommand = LlamaLoad(
      path: modelPath,
      modelParams: ModelParams(),
      contextParams: ContextParams()
        ..nCtx = contextLength
        ..nBatch = 512,
      samplingParams: SamplerParams()
        ..temp = temperature
        ..topP = 0.9
        ..topK = 40,
    );

    _parent = LlamaParent(loadCommand);
    await _parent!.init();
    _ready = true;
  }

  // ─── 채팅 ────────────────────────────────────────────────────
  Future<String> chat({
    required Character character,
    required List<Message> history,
    required String userMessage,
    int historyCount = 10,
  }) async {
    if (!_ready || _parent == null) {
      throw Exception('AI가 초기화되지 않았습니다. 앱을 재시작해주세요.');
    }
    if (_generating) {
      throw Exception('이미 생성 중입니다.');
    }

    _generating = true;
    final buffer = StringBuffer();

    // 토큰 스트림을 미리 구독 (sendPrompt 전에 구독해야 토큰 유실 없음)
    final tokenSub = _parent!.stream.listen((token) {
      if (!_isStopToken(token)) buffer.write(token);
    });

    try {
      final prompt = _buildPrompt(character, history, userMessage, historyCount);
      final promptId = await _parent!.sendPrompt(prompt);
      await _parent!
          .waitForCompletion(promptId)
          .timeout(const Duration(seconds: 120));

      return _clean(buffer.toString());
    } finally {
      await tokenSub.cancel();
      _generating = false;
    }
  }

  // ─── Llama 3.2 Instruct 채팅 템플릿 ─────────────────────────
  String _buildPrompt(
    Character character,
    List<Message> history,
    String userMessage,
    int historyCount,
  ) {
    final sb = StringBuffer();

    sb.write('<|begin_of_text|>');
    sb.write('<|start_header_id|>system<|end_header_id|>\n\n');
    sb.write(character.buildSystemPrompt());
    sb.write('<|eot_id|>');

    for (final msg in history.takeLast(historyCount)) {
      final role = msg.isUser ? 'user' : 'assistant';
      sb.write('<|start_header_id|>$role<|end_header_id|>\n\n');
      sb.write(msg.content);
      sb.write('<|eot_id|>');
    }

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

  Future<void> _dispose() async {
    await _parent?.dispose();
    _parent = null;
    _ready = false;
    _generating = false;
  }

  Future<void> dispose() => _dispose();
}

extension _ListTakeLast<T> on List<T> {
  List<T> takeLast(int n) =>
      length <= n ? this : sublist(length - n);
}
