import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../../models/character.dart';
import '../../../models/message.dart';

class MessageBubble extends StatelessWidget {
  final Message message;
  final Character character;
  final String? highlight;

  const MessageBubble({
    super.key,
    required this.message,
    required this.character,
    this.highlight,
  });

  @override
  Widget build(BuildContext context) {
    final isUser = message.isUser;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final bubbleColor = isUser
        ? const Color(0xFF7C5CBF)
        : (isDark ? const Color(0xFF2E2544) : Colors.white);
    final textColor =
        isUser ? Colors.white : (isDark ? const Color(0xFFEDE8FF) : const Color(0xFF2D2040));

    return Semantics(
      label: '${isUser ? "나" : character.name}: ${message.content}',
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        child: Row(
          mainAxisAlignment:
              isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            if (!isUser) ...[
              _Avatar(character: character),
              const SizedBox(width: 8),
            ],
            Flexible(
              child: Container(
                padding: const EdgeInsets.symmetric(
                    horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: bubbleColor,
                  borderRadius: BorderRadius.only(
                    topLeft: const Radius.circular(20),
                    topRight: const Radius.circular(20),
                    bottomLeft: Radius.circular(isUser ? 20 : 4),
                    bottomRight: Radius.circular(isUser ? 4 : 20),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.06),
                      blurRadius: 8,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: highlight != null && highlight!.isNotEmpty
                    ? _HighlightedText(
                        text: message.content,
                        query: highlight!,
                        baseStyle: TextStyle(
                          fontSize: 15,
                          height: 1.5,
                          color: textColor,
                        ),
                      )
                    : Text(
                        message.content,
                        style: TextStyle(
                          fontSize: 15,
                          height: 1.5,
                          color: textColor,
                        ),
                      ),
              ),
            ),
            if (isUser) const SizedBox(width: 8),
          ],
        ),
      ),
    ).animate().fadeIn(duration: 250.ms).slideY(begin: 0.1, end: 0);
  }
}

class _HighlightedText extends StatelessWidget {
  final String text;
  final String query;
  final TextStyle baseStyle;

  const _HighlightedText({
    required this.text,
    required this.query,
    required this.baseStyle,
  });

  @override
  Widget build(BuildContext context) {
    final lowerText = text.toLowerCase();
    final lowerQuery = query.toLowerCase();
    final spans = <TextSpan>[];
    int start = 0;

    while (true) {
      final idx = lowerText.indexOf(lowerQuery, start);
      if (idx == -1) {
        spans.add(TextSpan(text: text.substring(start)));
        break;
      }
      if (idx > start) {
        spans.add(TextSpan(text: text.substring(start, idx)));
      }
      spans.add(TextSpan(
        text: text.substring(idx, idx + query.length),
        style: baseStyle.copyWith(
          backgroundColor: const Color(0xFFFFD93D).withOpacity(0.6),
          color: const Color(0xFF2D2040),
          fontWeight: FontWeight.w700,
        ),
      ));
      start = idx + query.length;
    }

    return RichText(
      text: TextSpan(style: baseStyle, children: spans),
    );
  }
}

class _Avatar extends StatelessWidget {
  final Character character;
  const _Avatar({required this.character});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        color: character.appearance.color.withOpacity(0.25),
        shape: BoxShape.circle,
      ),
      child: Center(
        child: Text(
          character.avatarEmoji,
          style: const TextStyle(fontSize: 16),
        ),
      ),
    );
  }
}

class TypingIndicator extends StatelessWidget {
  final Character character;

  const TypingIndicator({super.key, required this.character});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      child: Row(
        children: [
          _Avatar(character: character),
          const SizedBox(width: 8),
          Container(
            padding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF2E2544) : Colors.white,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(20),
                topRight: Radius.circular(20),
                bottomLeft: Radius.circular(4),
                bottomRight: Radius.circular(20),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.06),
                  blurRadius: 8,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: List.generate(
                3,
                (i) => Container(
                  width: 8,
                  height: 8,
                  margin: const EdgeInsets.symmetric(horizontal: 2),
                  decoration: BoxDecoration(
                    color: const Color(0xFF7C5CBF).withOpacity(0.6),
                    shape: BoxShape.circle,
                  ),
                )
                    .animate(onPlay: (c) => c.repeat())
                    .moveY(
                      begin: 0,
                      end: -6,
                      delay: (i * 150).ms,
                      duration: 400.ms,
                      curve: Curves.easeInOut,
                    )
                    .then()
                    .moveY(begin: -6, end: 0, duration: 400.ms),
              ),
            ),
          ),
        ],
      ),
    ).animate().fadeIn(duration: 200.ms);
  }
}
