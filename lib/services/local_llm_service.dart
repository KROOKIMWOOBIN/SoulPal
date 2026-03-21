import 'dart:async';
import 'package:llama_cpp_dart/llama_cpp_dart.dart' hide Message;
import '../models/character.dart';
import '../models/message.dart';

class LocalLlmService {
  Llama? _llama;
  bool _ready = false;
  bool _generating = false;

  bool get isReady => _ready;

  // ─── 초기화 ──────────────────────────────────────────────────
  Future<void> initialize(
    String modelPath, {
    double temperature = 0.8,
    int contextLength = 2048,
  }) async {
    _dispose();

    final contextParams = ContextParams()
      ..nCtx = contextLength
      ..nBatch = 512;

    final samplerParams = SamplerParams()
      ..temp = temperature
      ..topP = 0.9
      ..topK = 40;

    _llama = Llama(modelPath, null, contextParams, samplerParams);
    _ready = true;
  }

  // ─── 채팅 ────────────────────────────────────────────────────
  Future<String> chat({
    required Character character,
    required List<Message> history,
    required String userMessage,
    int historyCount = 10,
  }) async {
    if (!_ready || _llama == null) {
      throw Exception('AI가 초기화되지 않았습니다. 앱을 재시작해주세요.');
    }
    if (_generating) {
      throw Exception('이미 생성 중입니다.');
    }

    _generating = true;
    try {
      final prompt = _buildPrompt(character, history, userMessage, historyCount);
      _llama!.setPrompt(prompt);

      final buffer = StringBuffer();
      await for (final token in _llama!.generateText()) {
        if (_isStopToken(token)) break;
        buffer.write(token);
      }

      return _clean(buffer.toString());
    } finally {
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

  void _dispose() {
    _llama?.dispose();
    _llama = null;
    _ready = false;
    _generating = false;
  }

  void dispose() => _dispose();
}

extension _ListTakeLast<T> on List<T> {
  List<T> takeLast(int n) =>
      length <= n ? this : sublist(length - n);
}
