import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../app.dart';
import '../models/character.dart';
import '../services/storage_service.dart';

enum CharacterSortOrder { recent, name, favorite }

class CharacterProvider extends ChangeNotifier {
  final StorageService _storage;
  List<Character> _characters = [];
  CharacterSortOrder _sortOrder = CharacterSortOrder.recent;
  String? _lastError;

  CharacterProvider(SharedPreferences prefs)
      : _storage = StorageService(prefs) {
    try {
      _characters = _storage.loadCharacters();
    } catch (e) {
      _lastError = e.toString();
      showGlobalError('캐릭터 목록 로드 실패: $e');
    }
  }

  CharacterSortOrder get sortOrder => _sortOrder;
  String? get lastError => _lastError;

  List<Character> get characters {
    final list = List<Character>.from(_characters);
    switch (_sortOrder) {
      case CharacterSortOrder.recent:
        list.sort((a, b) {
          final aTime = a.lastMessageAt ?? a.createdAt;
          final bTime = b.lastMessageAt ?? b.createdAt;
          return bTime.compareTo(aTime);
        });
      case CharacterSortOrder.name:
        list.sort((a, b) => a.name.compareTo(b.name));
      case CharacterSortOrder.favorite:
        list.sort((a, b) {
          if (a.isFavorite && !b.isFavorite) return -1;
          if (!a.isFavorite && b.isFavorite) return 1;
          final aTime = a.lastMessageAt ?? a.createdAt;
          final bTime = b.lastMessageAt ?? b.createdAt;
          return bTime.compareTo(aTime);
        });
    }
    return List.unmodifiable(list);
  }

  void setSortOrder(CharacterSortOrder order) {
    _sortOrder = order;
    notifyListeners();
  }

  Character? getById(String id) {
    try {
      return _characters.firstWhere((c) => c.id == id);
    } catch (_) {
      return null;
    }
  }

  Future<void> addCharacter(Character character) async {
    _characters.add(character);
    notifyListeners();
    try {
      await _storage.saveCharacters(_characters);
    } catch (e) {
      showGlobalError('캐릭터 저장 실패: $e');
    }
  }

  Future<void> updateCharacter(Character updated) async {
    final idx = _characters.indexWhere((c) => c.id == updated.id);
    if (idx == -1) return;
    _characters[idx] = updated;
    notifyListeners();
    try {
      await _storage.saveCharacters(_characters);
    } catch (e) {
      showGlobalError('캐릭터 업데이트 실패: $e');
    }
  }

  Future<void> toggleFavorite(String characterId) async {
    final idx = _characters.indexWhere((c) => c.id == characterId);
    if (idx == -1) return;
    _characters[idx] =
        _characters[idx].copyWith(isFavorite: !_characters[idx].isFavorite);
    notifyListeners();
    try {
      await _storage.saveCharacters(_characters);
    } catch (e) {
      showGlobalError('즐겨찾기 저장 실패: $e');
    }
  }

  Future<void> updateLastMessage(
      String characterId, String message) async {
    final idx = _characters.indexWhere((c) => c.id == characterId);
    if (idx == -1) return;
    _characters[idx] = _characters[idx].copyWith(
      lastMessage: message,
      lastMessageAt: DateTime.now(),
    );
    notifyListeners();
    try {
      await _storage.saveCharacters(_characters);
    } catch (e) {
      // 마지막 메시지 저장 실패는 조용히 처리 (UX 방해 최소화)
      debugPrint('[CharacterProvider] updateLastMessage 저장 실패: $e');
    }
  }

  Future<void> deleteCharacter(String characterId) async {
    _characters.removeWhere((c) => c.id == characterId);
    notifyListeners();
    try {
      await _storage.deleteCharacter(characterId);
    } catch (e) {
      showGlobalError('캐릭터 삭제 실패: $e');
    }
  }
}
