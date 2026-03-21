import 'dart:convert';
import 'package:flutter/material.dart' show Color;
import '../core/constants/categories.dart';

class Character {
  final String id;
  String name;
  String relationshipId;
  String personalityId;
  String speechStyleId;
  List<String> interestIds;
  String appearanceId;
  DateTime createdAt;
  String? lastMessage;
  DateTime? lastMessageAt;
  bool isFavorite;

  Character({
    required this.id,
    required this.name,
    required this.relationshipId,
    required this.personalityId,
    required this.speechStyleId,
    required this.interestIds,
    required this.appearanceId,
    required this.createdAt,
    this.lastMessage,
    this.lastMessageAt,
    this.isFavorite = false,
  });

  // ─── Convenience getters ─────────────────────────────────────
  CategoryItem get relationship => CategoryData.getRelationship(relationshipId);
  CategoryItem get personality => CategoryData.getPersonality(personalityId);
  CategoryItem get speechStyle => CategoryData.getSpeechStyle(speechStyleId);
  CategoryItem get appearance => CategoryData.getAppearance(appearanceId);
  List<CategoryItem> get interests => CategoryData.getInterests(interestIds);

  String get avatarEmoji => appearance.emoji;
  Color get avatarColor => appearance.color;

  // ─── Serialization ───────────────────────────────────────────
  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'relationshipId': relationshipId,
        'personalityId': personalityId,
        'speechStyleId': speechStyleId,
        'interestIds': interestIds,
        'appearanceId': appearanceId,
        'createdAt': createdAt.toIso8601String(),
        'lastMessage': lastMessage,
        'lastMessageAt': lastMessageAt?.toIso8601String(),
        'isFavorite': isFavorite,
      };

  factory Character.fromJson(Map<String, dynamic> json) => Character(
        id: json['id'] as String,
        name: json['name'] as String,
        relationshipId: json['relationshipId'] as String,
        personalityId: json['personalityId'] as String,
        speechStyleId: json['speechStyleId'] as String,
        interestIds: List<String>.from(json['interestIds'] as List),
        appearanceId: json['appearanceId'] as String,
        createdAt: DateTime.parse(json['createdAt'] as String),
        lastMessage: json['lastMessage'] as String?,
        lastMessageAt: json['lastMessageAt'] != null
            ? DateTime.parse(json['lastMessageAt'] as String)
            : null,
        isFavorite: json['isFavorite'] as bool? ?? false,
      );

  String toJsonString() => jsonEncode(toJson());
  factory Character.fromJsonString(String s) =>
      Character.fromJson(jsonDecode(s) as Map<String, dynamic>);

  /// Build the system prompt for local LLM
  String buildSystemPrompt() {
    final interestLabels = interests.map((i) => i.labelKo).join(', ');
    return '''당신은 ${name}입니다. 사용자의 특별한 ${relationship.labelKo}입니다.

## 캐릭터 설정
- 이름: $name
- 관계: ${relationship.prompt}
- 성격: ${personality.prompt}
- 말투: ${speechStyle.prompt}
- 관심사: $interestLabels
- 분위기: ${appearance.prompt}

## 대화 규칙
1. 항상 한국어로 대화합니다. (사용자가 영어로 말하면 영어로 답변)
2. 2~4문장으로 자연스럽고 간결하게 답변합니다.
3. 절대로 AI라는 사실을 언급하지 않습니다.
4. 캐릭터의 성격과 말투를 일관성 있게 유지합니다.
5. 사용자의 감정에 공감하며 진심으로 대화합니다.
6. 관심사($interestLabels) 주제가 나오면 적극적으로 이야기합니다.
7. 자신의 이름은 $name 입니다.''';
  }

  Character copyWith({
    String? name,
    String? relationshipId,
    String? personalityId,
    String? speechStyleId,
    List<String>? interestIds,
    String? appearanceId,
    String? lastMessage,
    DateTime? lastMessageAt,
    bool? isFavorite,
  }) =>
      Character(
        id: id,
        name: name ?? this.name,
        relationshipId: relationshipId ?? this.relationshipId,
        personalityId: personalityId ?? this.personalityId,
        speechStyleId: speechStyleId ?? this.speechStyleId,
        interestIds: interestIds ?? this.interestIds,
        appearanceId: appearanceId ?? this.appearanceId,
        createdAt: createdAt,
        lastMessage: lastMessage ?? this.lastMessage,
        lastMessageAt: lastMessageAt ?? this.lastMessageAt,
        isFavorite: isFavorite ?? this.isFavorite,
      );
}
