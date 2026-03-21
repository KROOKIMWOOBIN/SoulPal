import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';
import '../models/character.dart';
import '../models/message.dart';
import '../services/local_llm_service.dart';
import '../services/storage_service.dart';

enum ChatStatus { idle, loading, error }

class ChatProvider extends ChangeNotifier {
  final StorageService _storage;
  final LocalLlmService _llm = LocalLlmService();
  final _uuid = const Uuid();

  String? _currentCharacterId;
  List<Message> _messages = [];
  ChatStatus _status = ChatStatus.idle;
  String? _errorMessage;

  ChatProvider(SharedPreferences prefs)
      : _storage = StorageService(prefs);

  List<Message> get messages => List.unmodifiable(_messages);
  ChatStatus get status => _status;
  String? get errorMessage => _errorMessage;
  bool get isLoading => _status == ChatStatus.loading;
  bool get isLlmReady => _llm.isReady;

  // ─── LLM 초기화 (splash에서 호출) ───────────────────────────
  Future<void> initializeLlm(String modelPath) async {
    await _llm.initialize(modelPath);
    notifyListeners();
  }

  // ─── 채팅 로드 ───────────────────────────────────────────────
  void loadChat(String characterId) {
    if (_currentCharacterId == characterId) return;
    _currentCharacterId = characterId;
    _messages = _storage.loadMessages(characterId);
    _status = ChatStatus.idle;
    _errorMessage = null;
    notifyListeners();
  }

  // ─── 메시지 전송 ─────────────────────────────────────────────
  Future<void> sendMessage(Character character, String text) async {
    if (text.trim().isEmpty || _status == ChatStatus.loading) return;

    final userMsg = Message(
      id: _uuid.v4(),
      characterId: character.id,
      content: text.trim(),
      isUser: true,
      timestamp: DateTime.now(),
    );

    _messages.add(userMsg);
    _status = ChatStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final reply = await _llm.chat(
        character: character,
        history: _messages.sublist(0, _messages.length - 1),
        userMessage: text.trim(),
      );

      _messages.add(Message(
        id: _uuid.v4(),
        characterId: character.id,
        content: reply,
        isUser: false,
        timestamp: DateTime.now(),
      ));

      _status = ChatStatus.idle;
      await _storage.saveMessages(character.id, _messages);
    } catch (e) {
      _status = ChatStatus.error;
      _errorMessage = e.toString();
    }

    notifyListeners();
  }

  Future<void> clearHistory(String characterId) async {
    _messages.clear();
    await _storage.clearMessages(characterId);
    notifyListeners();
  }

  void clearError() {
    _status = ChatStatus.idle;
    _errorMessage = null;
    notifyListeners();
  }

  @override
  void dispose() {
    _llm.dispose();
    super.dispose();
  }
}
