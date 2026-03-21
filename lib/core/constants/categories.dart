import 'package:flutter/material.dart';

class CategoryItem {
  final String id;
  final String emoji;
  final String labelKo;
  final String labelEn;
  final Color color;
  final String prompt;

  const CategoryItem({
    required this.id,
    required this.emoji,
    required this.labelKo,
    required this.labelEn,
    required this.color,
    required this.prompt,
  });

  String label(String locale) => locale.startsWith('ko') ? labelKo : labelEn;
}

class CategoryData {
  // ─── 관계 유형 ───────────────────────────────────────────────
  static const List<CategoryItem> relationships = [
    CategoryItem(
      id: 'best_friend',
      emoji: '🤝',
      labelKo: '베프',
      labelEn: 'Best Friend',
      color: Color(0xFFFF6B6B),
      prompt: 'a best friend who is honest, comfortable, and always there',
    ),
    CategoryItem(
      id: 'mentor',
      emoji: '🎓',
      labelKo: '멘토',
      labelEn: 'Mentor',
      color: Color(0xFF4ECDC4),
      prompt: 'a wise mentor who guides, supports, and gives thoughtful advice',
    ),
    CategoryItem(
      id: 'romantic',
      emoji: '💝',
      labelKo: '연인',
      labelEn: 'Romantic',
      color: Color(0xFFFF85A1),
      prompt: 'a romantic partner who is caring, warm, and affectionate',
    ),
    CategoryItem(
      id: 'older_sibling',
      emoji: '👑',
      labelKo: '언니/오빠',
      labelEn: 'Older Sibling',
      color: Color(0xFF6C5CE7),
      prompt: 'a protective and caring older sibling',
    ),
    CategoryItem(
      id: 'younger_sibling',
      emoji: '🐣',
      labelKo: '동생',
      labelEn: 'Younger Sibling',
      color: Color(0xFFFFD93D),
      prompt: 'an adorable and energetic younger sibling',
    ),
    CategoryItem(
      id: 'classmate',
      emoji: '📚',
      labelKo: '학교 친구',
      labelEn: 'Classmate',
      color: Color(0xFF95E1D3),
      prompt: 'a classmate who shares experiences, memories, and daily life',
    ),
  ];

  // ─── 성격 ────────────────────────────────────────────────────
  static const List<CategoryItem> personalities = [
    CategoryItem(
      id: 'energetic',
      emoji: '⚡',
      labelKo: '활발한',
      labelEn: 'Energetic',
      color: Color(0xFFFF9F43),
      prompt: 'energetic, lively, and enthusiastic — always full of positive energy',
    ),
    CategoryItem(
      id: 'calm',
      emoji: '🌊',
      labelKo: '차분한',
      labelEn: 'Calm',
      color: Color(0xFF74B9FF),
      prompt: 'calm, peaceful, and steady — never rushed, always composed',
    ),
    CategoryItem(
      id: 'humorous',
      emoji: '😄',
      labelKo: '유머러스한',
      labelEn: 'Humorous',
      color: Color(0xFFFDCB6E),
      prompt: 'funny, witty, and charming — always ready with a joke or playful comment',
    ),
    CategoryItem(
      id: 'serious',
      emoji: '🎯',
      labelKo: '진지한',
      labelEn: 'Serious',
      color: Color(0xFF636E72),
      prompt: 'serious, thoughtful, and sincere — takes conversations deeply',
    ),
    CategoryItem(
      id: 'empathetic',
      emoji: '💚',
      labelKo: '공감적인',
      labelEn: 'Empathetic',
      color: Color(0xFF00B894),
      prompt: 'deeply empathetic and emotionally supportive — always validates feelings',
    ),
    CategoryItem(
      id: 'adventurous',
      emoji: '🌟',
      labelKo: '도전적인',
      labelEn: 'Adventurous',
      color: Color(0xFFE17055),
      prompt: 'adventurous, bold, and motivating — always pushes to try new things',
    ),
  ];

  // ─── 말투 ────────────────────────────────────────────────────
  static const List<CategoryItem> speechStyles = [
    CategoryItem(
      id: 'casual',
      emoji: '😊',
      labelKo: '친근한 반말',
      labelEn: 'Casual',
      color: Color(0xFFFF7675),
      prompt: 'uses casual Korean (반말), very friendly and informal like a close friend',
    ),
    CategoryItem(
      id: 'formal',
      emoji: '🌸',
      labelKo: '다정한 존댓말',
      labelEn: 'Warm Formal',
      color: Color(0xFFBB8FCE),
      prompt: 'uses polite Korean (존댓말) but remains warm, gentle, and caring',
    ),
    CategoryItem(
      id: 'playful',
      emoji: '🎪',
      labelKo: '장난스러운',
      labelEn: 'Playful',
      color: Color(0xFFFFA07A),
      prompt: 'playful and teasing speech with lots of fun expressions and emoticons',
    ),
    CategoryItem(
      id: 'encouraging',
      emoji: '💪',
      labelKo: '격려하는',
      labelEn: 'Encouraging',
      color: Color(0xFF82E0AA),
      prompt: 'always encouraging, motivating, and cheering the user on',
    ),
  ];

  // ─── 관심사 (다중 선택) ──────────────────────────────────────
  static const List<CategoryItem> interests = [
    CategoryItem(id: 'music',   emoji: '🎵', labelKo: '음악',   labelEn: 'Music',   color: Color(0xFFFF6B6B), prompt: 'music'),
    CategoryItem(id: 'sports',  emoji: '⚽', labelKo: '운동',   labelEn: 'Sports',  color: Color(0xFF4ECDC4), prompt: 'sports and exercise'),
    CategoryItem(id: 'reading', emoji: '📖', labelKo: '독서',   labelEn: 'Reading', color: Color(0xFF6C5CE7), prompt: 'books and reading'),
    CategoryItem(id: 'gaming',  emoji: '🎮', labelKo: '게임',   labelEn: 'Gaming',  color: Color(0xFFFF9F43), prompt: 'gaming and esports'),
    CategoryItem(id: 'cooking', emoji: '🍳', labelKo: '요리',   labelEn: 'Cooking', color: Color(0xFF00B894), prompt: 'cooking and food'),
    CategoryItem(id: 'travel',  emoji: '✈️', labelKo: '여행',   labelEn: 'Travel',  color: Color(0xFF0984E3), prompt: 'travel and adventure'),
    CategoryItem(id: 'art',     emoji: '🎨', labelKo: '예술',   labelEn: 'Art',     color: Color(0xFFE17055), prompt: 'art and creativity'),
    CategoryItem(id: 'movies',  emoji: '🎬', labelKo: '영화',   labelEn: 'Movies',  color: Color(0xFF636E72), prompt: 'movies and dramas'),
  ];

  // ─── 외모/분위기 ─────────────────────────────────────────────
  static const List<CategoryItem> appearances = [
    CategoryItem(
      id: 'cute',
      emoji: '🐱',
      labelKo: '귀여운',
      labelEn: 'Cute',
      color: Color(0xFFFFB5C8),
      prompt: 'cute',
    ),
    CategoryItem(
      id: 'cool',
      emoji: '🌙',
      labelKo: '시크한',
      labelEn: 'Cool',
      color: Color(0xFF6C5CE7),
      prompt: 'cool and chic',
    ),
    CategoryItem(
      id: 'warm',
      emoji: '☀️',
      labelKo: '따뜻한',
      labelEn: 'Warm',
      color: Color(0xFFFFD93D),
      prompt: 'warm and cozy',
    ),
    CategoryItem(
      id: 'sporty',
      emoji: '🔥',
      labelKo: '활발한',
      labelEn: 'Sporty',
      color: Color(0xFFFF6B6B),
      prompt: 'sporty and energetic',
    ),
    CategoryItem(
      id: 'intellectual',
      emoji: '🦉',
      labelKo: '지적인',
      labelEn: 'Intellectual',
      color: Color(0xFF4ECDC4),
      prompt: 'intellectual and elegant',
    ),
    CategoryItem(
      id: 'natural',
      emoji: '🌿',
      labelKo: '자연스러운',
      labelEn: 'Natural',
      color: Color(0xFF95E1D3),
      prompt: 'natural and refreshing',
    ),
  ];

  // ─── Helper methods ──────────────────────────────────────────
  static CategoryItem? findById(List<CategoryItem> list, String id) {
    try {
      return list.firstWhere((item) => item.id == id);
    } catch (_) {
      return null;
    }
  }

  static CategoryItem getRelationship(String id) =>
      findById(relationships, id) ?? relationships.first;

  static CategoryItem getPersonality(String id) =>
      findById(personalities, id) ?? personalities.first;

  static CategoryItem getSpeechStyle(String id) =>
      findById(speechStyles, id) ?? speechStyles.first;

  static CategoryItem getAppearance(String id) =>
      findById(appearances, id) ?? appearances.first;

  static List<CategoryItem> getInterests(List<String> ids) =>
      ids.map((id) => findById(interests, id)).whereType<CategoryItem>().toList();
}
