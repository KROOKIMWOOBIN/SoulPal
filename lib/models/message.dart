import 'dart:convert';

class Message {
  final String id;
  final String characterId;
  final String content;
  final bool isUser;
  final DateTime timestamp;

  const Message({
    required this.id,
    required this.characterId,
    required this.content,
    required this.isUser,
    required this.timestamp,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'characterId': characterId,
        'content': content,
        'isUser': isUser,
        'timestamp': timestamp.toIso8601String(),
      };

  factory Message.fromJson(Map<String, dynamic> json) => Message(
        id: json['id'] as String,
        characterId: json['characterId'] as String,
        content: json['content'] as String,
        isUser: json['isUser'] as bool,
        timestamp: DateTime.parse(json['timestamp'] as String),
      );

  /// Convert to Ollama API message format
  Map<String, String> toOllamaMessage() => {
        'role': isUser ? 'user' : 'assistant',
        'content': content,
      };

  static List<Message> fromJsonList(String s) {
    final list = jsonDecode(s) as List;
    return list.map((e) => Message.fromJson(e as Map<String, dynamic>)).toList();
  }

  static String toJsonList(List<Message> messages) =>
      jsonEncode(messages.map((m) => m.toJson()).toList());
}
