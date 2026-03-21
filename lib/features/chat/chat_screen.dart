import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/character.dart';
import '../../providers/chat_provider.dart';
import '../../providers/character_provider.dart';
import '../../providers/settings_provider.dart';
import 'widgets/chat_input.dart';
import 'widgets/message_bubble.dart';

class ChatScreen extends StatefulWidget {
  final Character character;

  const ChatScreen({super.key, required this.character});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final chat = context.read<ChatProvider>();
      chat.loadChat(widget.character.id);
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final chat = context.watch<ChatProvider>();
    final appearance = widget.character.appearance;

    return Scaffold(
      backgroundColor: const Color(0xFFF5F0FF),
      appBar: AppBar(
        backgroundColor: Colors.white,
        titleSpacing: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded),
          onPressed: () => Navigator.pop(context),
        ),
        title: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: appearance.color.withOpacity(0.25),
                shape: BoxShape.circle,
              ),
              child: Center(
                child: Text(
                  widget.character.avatarEmoji,
                  style: const TextStyle(fontSize: 20),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.character.name,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Text(
                  widget.character.relationship.label(settings.locale),
                  style: TextStyle(
                    fontSize: 12,
                    color: appearance.color,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ],
        ),
        actions: [
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert_rounded),
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16)),
            onSelected: (val) {
              if (val == 'clear') _confirmClear(context, settings);
            },
            itemBuilder: (_) => [
              PopupMenuItem(
                value: 'clear',
                child: Row(
                  children: [
                    const Icon(Icons.delete_outline, color: Colors.red),
                    const SizedBox(width: 8),
                    Text(settings.t('대화 초기화', 'Clear chat')),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
      body: Column(
        children: [
          // Error banner
          if (chat.status == ChatStatus.error && chat.errorMessage != null)
            _ErrorBanner(
              message: chat.errorMessage!,
              onDismiss: () => chat.clearError(),
            ),
          // Messages
          Expanded(
            child: _buildMessageList(chat),
          ),
          // Input
          ChatInput(
            enabled: !chat.isLoading,
            onSend: (text) {
              chat.sendMessage(widget.character, text).then((_) {
                // Update last message on character card
                if (mounted && chat.messages.isNotEmpty) {
                  final lastAi = chat.messages.lastWhere(
                    (m) => !m.isUser,
                    orElse: () => chat.messages.last,
                  );
                  context
                      .read<CharacterProvider>()
                      .updateLastMessage(widget.character.id, lastAi.content);
                }
              });
              _scrollToBottom();
            },
          ),
        ],
      ),
    );
  }

  Widget _buildMessageList(ChatProvider chat) {
    final messages = chat.messages;
    final isLoading = chat.isLoading;

    if (messages.isEmpty && !isLoading) {
      return _WelcomeMessage(character: widget.character);
    }

    WidgetsBinding.instance.addPostFrameCallback((_) => _scrollToBottom());

    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.symmetric(vertical: 12),
      itemCount: messages.length + (isLoading ? 1 : 0),
      itemBuilder: (_, i) {
        if (i == messages.length && isLoading) {
          return TypingIndicator(character: widget.character);
        }
        return MessageBubble(
          message: messages[i],
          character: widget.character,
        );
      },
    );
  }

  void _confirmClear(BuildContext context, SettingsProvider settings) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(settings.t('대화 초기화', 'Clear Chat')),
        content: Text(settings.t(
          '모든 대화 기록이 삭제됩니다.',
          'All chat history will be deleted.',
        )),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(settings.t('취소', 'Cancel')),
          ),
          TextButton(
            onPressed: () {
              context.read<ChatProvider>().clearHistory(widget.character.id);
              Navigator.pop(ctx);
            },
            child: Text(
              settings.t('초기화', 'Clear'),
              style: const TextStyle(color: Colors.red),
            ),
          ),
        ],
      ),
    );
  }
}

class _WelcomeMessage extends StatelessWidget {
  final Character character;
  const _WelcomeMessage({required this.character});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 88,
            height: 88,
            decoration: BoxDecoration(
              color: character.appearance.color.withOpacity(0.2),
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                character.avatarEmoji,
                style: const TextStyle(fontSize: 44),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            character.name,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            '${character.relationship.labelKo} • ${character.personality.labelKo}',
            style: const TextStyle(
              fontSize: 13,
              color: Color(0xFF7B6F8A),
            ),
          ),
          const SizedBox(height: 20),
          Text(
            '첫 인사를 건네보세요! 👋',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade500,
            ),
          ),
        ],
      ),
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  final String message;
  final VoidCallback onDismiss;

  const _ErrorBanner({required this.message, required this.onDismiss});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      color: const Color(0xFFFFF3F3),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: Colors.red, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(fontSize: 13, color: Colors.red),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 18, color: Colors.red),
            padding: EdgeInsets.zero,
            onPressed: onDismiss,
          ),
        ],
      ),
    );
  }
}
