import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/character.dart';
import '../services/storage_service.dart';

class CharacterProvider extends ChangeNotifier {
  final StorageService _storage;
  List<Character> _characters = [];

  CharacterProvider(SharedPreferences prefs)
      : _storage = StorageService(prefs) {
    _characters = _storage.loadCharacters();
  }

  List<Character> get characters => List.unmodifiable(_characters);

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
