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

  // pagination
  static const int _pageSize = 30;
  int _loadedCount = _pageSize;
  bool _hasMore = false;

  // search
  String _searchQuery = '';

  ChatProvider(SharedPreferences prefs)
      : _storage = StorageService(prefs);

  List<Message> get messages {
    if (_searchQuery.isEmpty) {
      return List.unmodifiable(_messages);
    }
    final q = _searchQuery.toLowerCase();
    return _messages
        .where((m) => m.content.toLowerCase().contains(q))
        .toList();
  }

  List<Message> get allMessages => List.unmodifiable(_messages);
  ChatStatus get status => _status;
  String? get errorMessage => _errorMessage;
  bool get isLoading => _status == ChatStatus.loading;
  bool get isLlmReady => _llm.isReady;
  bool get hasMore => _hasMore;
  String get searchQuery => _searchQuery;
  bool get isSearching => _searchQuery.isNotEmpty;

  // ─── LLM 초기화 ──────────────────────────────────────────────
  Future<void> initializeLlm(
    String modelPath, {
    double temperature = 0.8,
    int contextLength = 2048,
  }) async {
    await _llm.initialize(
      modelPath,
      temperature: temperature,
      contextLength: contextLength,
    );
    notifyListeners();
  }

  // ─── 채팅 로드 (페이지네이션) ────────────────────────────────
  void loadChat(String characterId) {
    if (_currentCharacterId == characterId) {
      // 같은 캐릭터 재진입 시에도 검색 상태는 초기화
      if (_searchQuery.isNotEmpty) {
        _searchQuery = '';
        notifyListeners();
      }
      return;
    }
    _currentCharacterId = characterId;
    _searchQuery = '';
    final all = _storage.loadMessages(characterId);
    _hasMore = all.length > _pageSize;
    _loadedCount = _hasMore ? _pageSize : all.length;
    _messages = _hasMore
        ? all.sublist(all.length - _loadedCount)
        : List.from(all);
    _status = ChatStatus.idle;
    _errorMessage = null;
    notifyListeners();
  }

  void loadMoreMessages() {
    if (!_hasMore || _currentCharacterId == null) return;
    final all = _storage.loadMessages(_currentCharacterId!);
    final newCount = (_loadedCount + _pageSize).clamp(0, all.length);
    _hasMore = newCount < all.length;
    _loadedCount = newCount;
    _messages = all.sublist(all.length - _loadedCount);
    notifyListeners();
  }

  // ─── 검색 ────────────────────────────────────────────────────
  void setSearch(String query) {
    _searchQuery = query;
    notifyListeners();
  }

  void clearSearch() {
    _searchQuery = '';
    notifyListeners();
  }

  // ─── 메시지 전송 ─────────────────────────────────────────────
  Future<void> sendMessage(
    Character character,
    String text, {
    int historyCount = 10,
  }) async {
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
        history: _messages.length > 1
            ? _messages.sublist(0, _messages.length - 1)
            : [],
        userMessage: text.trim(),
        historyCount: historyCount,
      );

      // 응답 대기 중 다른 캐릭터로 이동한 경우 결과 폐기
      if (_currentCharacterId != character.id) return;

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
      if (_currentCharacterId != character.id) return;
      _status = ChatStatus.error;
      _errorMessage = e.toString();
    }

    notifyListeners();
  }

  // ─── AI 응답 재생성 ──────────────────────────────────────────
  Future<void> regenerateLastResponse(
    Character character, {
    int historyCount = 10,
  }) async {
    if (_status == ChatStatus.loading) return;

    // 마지막 AI 메시지 제거
    if (_messages.isNotEmpty && !_messages.last.isUser) {
      _messages.removeLast();
    }

    if (_messages.isEmpty || !_messages.last.isUser) return;

    final lastUserMsg = _messages.last;
    _messages.removeLast();

    _status = ChatStatus.loading;
    _errorMessage = null;
    notifyListeners();

    // 유저 메시지 다시 추가
    _messages.add(lastUserMsg);

    try {
      final reply = await _llm.chat(
        character: character,
        history: _messages.sublist(0, _messages.length - 1),
        userMessage: lastUserMsg.content,
        historyCount: historyCount,
      );

      if (_currentCharacterId != character.id) return;

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
      if (_currentCharacterId != character.id) return;
      _status = ChatStatus.error;
      _errorMessage = e.toString();
    }

    notifyListeners();
  }

  Future<void> clearHistory(String characterId) async {
    _messages.clear();
    _hasMore = false;
    _loadedCount = _pageSize;
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
    _llm.dispose().ignore();
    super.dispose();
  }
}
