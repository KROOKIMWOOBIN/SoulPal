import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/character.dart';
import '../services/storage_service.dart';

enum CharacterSortOrder { recent, name, favorite }

class CharacterProvider extends ChangeNotifier {
  final StorageService _storage;
  List<Character> _characters = [];
  CharacterSortOrder _sortOrder = CharacterSortOrder.recent;

  CharacterProvider(SharedPreferences prefs)
      : _storage = StorageService(prefs) {
    _characters = _storage.loadCharacters();
  }

  CharacterSortOrder get sortOrder => _sortOrder;

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
    await _storage.saveCharacters(_characters);
    notifyListeners();
  }

  Future<void> updateCharacter(Character updated) async {
    final idx = _characters.indexWhere((c) => c.id == updated.id);
    if (idx == -1) return;
    _characters[idx] = updated;
    await _storage.saveCharacters(_characters);
    notifyListeners();
  }

  Future<void> toggleFavorite(String characterId) async {
    final idx = _characters.indexWhere((c) => c.id == characterId);
    if (idx == -1) return;
    _characters[idx] =
        _characters[idx].copyWith(isFavorite: !_characters[idx].isFavorite);
    await _storage.saveCharacters(_characters);
    notifyListeners();
  }

  Future<void> updateLastMessage(
      String characterId, String message) async {
    final idx = _characters.indexWhere((c) => c.id == characterId);
    if (idx == -1) return;
    _characters[idx] = _characters[idx].copyWith(
      lastMessage: message,
      lastMessageAt: DateTime.now(),
    );
    await _storage.saveCharacters(_characters);
    notifyListeners();
  }

  Future<void> deleteCharacter(String characterId) async {
    _characters.removeWhere((c) => c.id == characterId);
    await _storage.deleteCharacter(characterId);
    notifyListeners();
  }
}
