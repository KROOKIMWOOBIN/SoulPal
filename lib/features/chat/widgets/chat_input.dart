import 'package:flutter/material.dart';

class ChatInput extends StatefulWidget {
  final bool enabled;
  final void Function(String) onSend;
  final VoidCallback? onRegenerate;

  const ChatInput({
    super.key,
    required this.enabled,
    required this.onSend,
    this.onRegenerate,
  });

  @override
  State<ChatInput> createState() => _ChatInputState();
}

class _ChatInputState extends State<ChatInput> {
  final _controller = TextEditingController();
  bool _hasText = false;

  @override
  void initState() {
    super.initState();
    _controller.addListener(() {
      final hasText = _controller.text.trim().isNotEmpty;
      if (hasText != _hasText) setState(() => _hasText = hasText);
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _send() {
    final text = _controller.text.trim();
    if (text.isEmpty || !widget.enabled) return;
    _controller.clear();
    widget.onSend(text);
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final bgColor = isDark ? const Color(0xFF241D35) : Colors.white;
    final inputBg = isDark ? const Color(0xFF2E2544) : const Color(0xFFF5F0FF);

    return Container(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
      decoration: BoxDecoration(
        color: bgColor,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.06),
            blurRadius: 12,
            offset: const Offset(0, -3),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Expanded(
                  child: Semantics(
                    label: widget.enabled
                        ? '메시지를 입력하세요'
                        : '답변을 기다리는 중',
                    child: TextField(
                      controller: _controller,
                      enabled: widget.enabled,
                      maxLines: 4,
                      minLines: 1,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _send(),
                      decoration: InputDecoration(
                        hintText: widget.enabled
                            ? '메시지를 입력하세요...'
                            : '답변을 기다리는 중...',
                        hintStyle: TextStyle(
                          fontSize: 14,
                          color: isDark
                              ? const Color(0xFF6B5F80)
                              : const Color(0xFFB0A8C8),
                        ),
                        filled: true,
                        fillColor: inputBg,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: BorderSide.none,
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: BorderSide.none,
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: const BorderSide(
                              color: Color(0xFF7C5CBF), width: 1.5),
                        ),
                        contentPadding: const EdgeInsets.symmetric(
                            horizontal: 18, vertical: 12),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                // 재생성 버튼
                if (widget.onRegenerate != null && !_hasText)
                  Semantics(
                    label: '마지막 AI 응답 재생성',
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      child: Material(
                        color: widget.enabled
                            ? const Color(0xFF7C5CBF).withOpacity(0.15)
                            : const Color(0xFFE0D8F0),
                        shape: const CircleBorder(),
                        child: InkWell(
                          onTap:
                              widget.enabled ? widget.onRegenerate : null,
                          customBorder: const CircleBorder(),
                          child: const Padding(
                            padding: EdgeInsets.all(12),
                            child: Icon(
                              Icons.refresh_rounded,
                              color: Color(0xFF7C5CBF),
                              size: 20,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                // 전송 버튼
                Semantics(
                  label: '메시지 전송',
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    child: Material(
                      color: _hasText && widget.enabled
                          ? const Color(0xFF7C5CBF)
                          : const Color(0xFFE0D8F0),
                      shape: const CircleBorder(),
                      child: InkWell(
                        onTap: _hasText && widget.enabled ? _send : null,
                        customBorder: const CircleBorder(),
                        child: const Padding(
                          padding: EdgeInsets.all(12),
                          child: Icon(
                            Icons.send_rounded,
                            color: Colors.white,
                            size: 20,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
