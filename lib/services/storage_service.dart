import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/character.dart';
import '../models/message.dart';

class StorageService {
  static const _charactersKey = 'soulpal_characters';
  static const _messagesPrefix = 'soulpal_messages_';

  final SharedPreferences _prefs;

  StorageService(this._prefs);

  // ─── Characters ──────────────────────────────────────────────
  List<Character> loadCharacters() {
    final raw = _prefs.getString(_charactersKey);
    if (raw == null) return [];
    final list = jsonDecode(raw) as List;
    return list
        .map((e) => Character.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> saveCharacters(List<Character> characters) async {
    final encoded = jsonEncode(characters.map((c) => c.toJson()).toList());
    await _prefs.setString(_charactersKey, encoded);
  }

  Future<void> deleteCharacter(String characterId) async {
    final characters = loadCharacters()
      ..removeWhere((c) => c.id == characterId);
    await saveCharacters(characters);
    await _prefs.remove('$_messagesPrefix$characterId');
  }

  // ─── Messages ────────────────────────────────────────────────
  List<Message> loadMessages(String characterId) {
    final raw = _prefs.getString('$_messagesPrefix$characterId');
    if (raw == null) return [];
    return Message.fromJsonList(raw);
  }

  Future<void> saveMessages(String characterId, List<Message> messages) async {
    // Keep only last 200 messages to avoid storage bloat
    final limited =
        messages.length > 200 ? messages.sublist(messages.length - 200) : messages;
    await _prefs.setString(
        '$_messagesPrefix$characterId', Message.toJsonList(limited));
  }

  Future<void> clearMessages(String characterId) async {
    await _prefs.remove('$_messagesPrefix$characterId');
  }

}
