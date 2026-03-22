import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../../models/character.dart';

class CharacterCard extends StatelessWidget {
  final Character character;
  final String locale;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  const CharacterCard({
    super.key,
    required this.character,
    required this.locale,
    required this.onTap,
    required this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    final appearance = character.appearance;
    final relationship = character.relationship;
    final personality = character.personality;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final cardColor = isDark ? const Color(0xFF2E2544) : Colors.white;

    return Semantics(
      label: '${character.name}, ${relationship.label(locale)}',
      hint: locale == 'ko' ? '탭하면 대화 시작, 길게 누르면 메뉴' : 'Tap to chat, long press for menu',
      button: true,
      child: GestureDetector(
        onTap: onTap,
        onLongPress: onLongPress,
        child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          decoration: BoxDecoration(
            color: cardColor,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: appearance.color.withOpacity(0.15),
                blurRadius: 16,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                // Avatar
                Stack(
                  children: [
                    Container(
                      width: 64,
                      height: 64,
                      decoration: BoxDecoration(
                        color: appearance.color.withOpacity(0.25),
                        shape: BoxShape.circle,
                      ),
                      child: Center(
                        child: Text(
                          character.avatarEmoji,
                          style: const TextStyle(fontSize: 30),
                        ),
                      ),
                    ),
                    // 즐겨찾기 배지
                    if (character.isFavorite)
                      Positioned(
                        right: 0,
                        bottom: 0,
                        child: Container(
                          width: 20,
                          height: 20,
                          decoration: BoxDecoration(
                            color: Colors.red,
                            shape: BoxShape.circle,
                            border: Border.all(
                                color: cardColor, width: 2),
                          ),
                          child: const Icon(
                            Icons.favorite,
                            color: Colors.white,
                            size: 10,
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(width: 14),
                // Info
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(
                            character.name,
                            style: const TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(width: 8),
                          _Tag(
                            label: relationship.label(locale),
                            color: relationship.color,
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          _Tag(
                            label: personality.label(locale),
                            color: personality.color,
                          ),
                          const SizedBox(width: 6),
                          _Tag(
                            label: appearance.label(locale),
                            color: appearance.color,
                          ),
                        ],
                      ),
                      if (character.lastMessage != null) ...[
                        const SizedBox(height: 6),
                        Text(
                          character.lastMessage!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 13,
                            color: Colors.grey.shade500,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
                // Arrow
                Icon(
                  Icons.chevron_right_rounded,
                  color: Colors.grey.shade400,
                ),
              ],
            ),
          ),
        ),
      ),
    ).animate().fadeIn(duration: 300.ms).slideX(begin: 0.05, end: 0);
  }
}

class _Tag extends StatelessWidget {
  final String label;
  final Color color;

  const _Tag({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(isDark ? 0.25 : 0.15),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: isDark ? color : color.withOpacity(0.9),
        ),
      ),
    );
  }
}
