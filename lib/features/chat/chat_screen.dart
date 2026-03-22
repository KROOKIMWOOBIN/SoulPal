import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:intl/intl.dart';
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
  final _searchController = TextEditingController();
  bool _showSearch = false;
  String? _initError;
  ChatProvider? _chatProvider;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      try {
        _chatProvider = context.read<ChatProvider>();
        _chatProvider!.loadChat(widget.character.id);
        _chatProvider!.addListener(_onChatUpdate);
      } catch (e) {
        if (mounted) setState(() => _initError = e.toString());
      }
    });
    _scrollController.addListener(_onScroll);
  }

  void _onChatUpdate() {
    if (!mounted) return;
    // 메시지 목록이 있고 로딩이 끝난 직후(응답 수신) 스크롤
    if (_chatProvider != null &&
        !_chatProvider!.isLoading &&
        _chatProvider!.messages.isNotEmpty) {
      _scrollToBottom();
    }
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;
    // 스크롤이 맨 위에 도달하면 더 불러오기
    if (_scrollController.position.pixels <=
        _scrollController.position.minScrollExtent + 80) {
      final chat = context.read<ChatProvider>();
      if (chat.hasMore) {
        chat.loadMoreMessages();
      }
    }
  }

  @override
  void dispose() {
    _chatProvider?.removeListener(_onChatUpdate);
    _scrollController.dispose();
    _searchController.dispose();
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
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: _showSearch
          ? _buildSearchAppBar(settings, chat)
          : _buildNormalAppBar(context, settings, chat, appearance),
      body: _initError != null
          ? _buildInitErrorBody(_initError!)
          : Column(
        children: [
          // 더 불러오기 인디케이터
          if (chat.hasMore)
            Semantics(
              label: settings.t('이전 메시지 로드 중', 'Loading earlier messages'),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const SizedBox(
                      width: 12,
                      height: 12,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      settings.t('이전 메시지 불러오는 중...', 'Loading earlier messages...'),
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ],
                ),
              ),
            ),

          // Error banner
          if (chat.status == ChatStatus.error &&
              chat.errorMessage != null)
            _ErrorBanner(
              message: chat.errorMessage!,
              onDismiss: () => chat.clearError(),
            ),

          // Messages
          Expanded(
            child: _buildMessageList(chat, settings),
          ),

          // Input (검색 중엔 숨김)
          if (!_showSearch)
            ChatInput(
              enabled: !chat.isLoading,
              onSend: (text) {
                final s = context.read<SettingsProvider>();
                final characterProvider = context.read<CharacterProvider>();
                chat
                    .sendMessage(
                      widget.character,
                      text,
                      historyCount: s.historyCount,
                    )
                    .then((_) {
                  if (mounted && chat.allMessages.isNotEmpty) {
                    final lastAi = chat.allMessages.lastWhere(
                      (m) => !m.isUser,
                      orElse: () => chat.allMessages.last,
                    );
                    characterProvider.updateLastMessage(
                        widget.character.id, lastAi.content);
                  }
                });
                _scrollToBottom();
              },
              onRegenerate: chat.allMessages.isNotEmpty &&
                      !chat.isLoading
                  ? () {
                      final s = context.read<SettingsProvider>();
                      chat.regenerateLastResponse(
                        widget.character,
                        historyCount: s.historyCount,
                      );
                    }
                  : null,
            ),
        ],
      ),
    );
  }

  Widget _buildInitErrorBody(String error) {
    final settings = context.read<SettingsProvider>();
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline_rounded,
                color: Colors.red, size: 48),
            const SizedBox(height: 16),
            Text(
              settings.t('채팅을 불러오지 못했습니다', 'Failed to load chat'),
              style: const TextStyle(
                  fontSize: 16, fontWeight: FontWeight.w700),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              error,
              style: const TextStyle(fontSize: 12, color: Colors.grey),
              textAlign: TextAlign.center,
              maxLines: 4,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () {
                setState(() => _initError = null);
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (!mounted) return;
                  try {
                    context.read<ChatProvider>().loadChat(widget.character.id);
                  } catch (e) {
                    if (mounted) setState(() => _initError = e.toString());
                  }
                });
              },
              icon: const Icon(Icons.refresh_rounded),
              label: Text(settings.t('다시 시도', 'Retry')),
            ),
          ],
        ),
      ),
    );
  }

  AppBar _buildNormalAppBar(
    BuildContext context,
    SettingsProvider settings,
    ChatProvider chat,
    dynamic appearance,
  ) {
    return AppBar(
      backgroundColor: Theme.of(context).appBarTheme.backgroundColor,
      titleSpacing: 0,
      leading: Semantics(
        label: settings.t('뒤로 가기', 'Go back'),
        child: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded),
          onPressed: () => Navigator.pop(context),
        ),
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
                widget.character.relationship
                    .label(settings.locale),
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
        Semantics(
          label: settings.t('검색', 'Search'),
          child: IconButton(
            icon: const Icon(Icons.search_rounded),
            onPressed: () => setState(() => _showSearch = true),
          ),
        ),
        Semantics(
          label: settings.t('더 보기', 'More options'),
          child: PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert_rounded),
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16)),
            onSelected: (val) {
              if (val == 'clear') _confirmClear(context, settings);
              if (val == 'export') _exportChat(context, settings, chat);
              if (val == 'copy') _copyChat(context, settings, chat);
            },
            itemBuilder: (_) => [
              PopupMenuItem(
                value: 'export',
                child: Row(
                  children: [
                    const Icon(Icons.share_rounded),
                    const SizedBox(width: 8),
                    Text(settings.t('대화 공유', 'Share chat')),
                  ],
                ),
              ),
              PopupMenuItem(
                value: 'copy',
                child: Row(
                  children: [
                    const Icon(Icons.copy_rounded),
                    const SizedBox(width: 8),
                    Text(settings.t('대화 복사', 'Copy chat')),
                  ],
                ),
              ),
              PopupMenuItem(
                value: 'clear',
                child: Row(
                  children: [
                    const Icon(Icons.delete_outline,
                        color: Colors.red),
                    const SizedBox(width: 8),
                    Text(settings.t('대화 초기화', 'Clear chat')),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  AppBar _buildSearchAppBar(
      SettingsProvider settings, ChatProvider chat) {
    return AppBar(
      leading: Semantics(
        label: settings.t('검색 닫기', 'Close search'),
        child: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded),
          onPressed: () {
            setState(() => _showSearch = false);
            _searchController.clear();
            chat.clearSearch();
          },
        ),
      ),
      title: TextField(
        controller: _searchController,
        autofocus: true,
        decoration: InputDecoration(
          hintText: settings.t('메시지 검색...', 'Search messages...'),
          border: InputBorder.none,
          contentPadding: EdgeInsets.zero,
        ),
        onChanged: (q) => chat.setSearch(q),
      ),
      actions: [
        if (_searchController.text.isNotEmpty)
          IconButton(
            icon: const Icon(Icons.clear_rounded),
            onPressed: () {
              _searchController.clear();
              chat.clearSearch();
            },
          ),
      ],
    );
  }

  Widget _buildMessageList(ChatProvider chat, SettingsProvider settings) {
    final messages = chat.messages;
    final isLoading = chat.isLoading;

    if (messages.isEmpty && !isLoading) {
      if (chat.isSearching) {
        return Center(
          child: Text(
            settings.t('검색 결과가 없어요.', 'No messages found.'),
            style: const TextStyle(color: Colors.grey),
          ),
        );
      }
      return _WelcomeMessage(character: widget.character);
    }

    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.symmetric(vertical: 12),
      itemCount: messages.length + (isLoading ? 1 : 0),
      itemBuilder: (_, i) {
        if (i == messages.length && isLoading) {
          return TypingIndicator(character: widget.character);
        }
        final msg = messages[i];
        return MessageBubble(
          message: msg,
          character: widget.character,
          highlight: chat.isSearching ? chat.searchQuery : null,
        );
      },
    );
  }

  void _exportChat(
      BuildContext context, SettingsProvider settings, ChatProvider chat) {
    final messages = chat.allMessages;
    if (messages.isEmpty) return;

    final fmt = DateFormat('yyyy-MM-dd HH:mm');
    final buf = StringBuffer();
    buf.writeln('=== ${widget.character.name} ===');
    buf.writeln();
    for (final m in messages) {
      final who = m.isUser
          ? settings.t('나', 'Me')
          : widget.character.name;
      buf.writeln('[${fmt.format(m.timestamp)}] $who');
      buf.writeln(m.content);
      buf.writeln();
    }
    Share.share(buf.toString(),
        subject:
            '${widget.character.name} ${settings.t("대화 기록", "Chat History")}');
  }

  void _copyChat(
      BuildContext context, SettingsProvider settings, ChatProvider chat) {
    final messages = chat.allMessages;
    if (messages.isEmpty) return;

    final fmt = DateFormat('yyyy-MM-dd HH:mm');
    final buf = StringBuffer();
    for (final m in messages) {
      final who = m.isUser
          ? settings.t('나', 'Me')
          : widget.character.name;
      buf.writeln('[$who ${fmt.format(m.timestamp)}] ${m.content}');
    }
    Clipboard.setData(ClipboardData(text: buf.toString()));
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content:
            Text(settings.t('대화가 복사됐어요!', 'Chat copied!')),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  void _confirmClear(
      BuildContext context, SettingsProvider settings) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20)),
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
              context
                  .read<ChatProvider>()
                  .clearHistory(widget.character.id);
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
    final settings = context.watch<SettingsProvider>();
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
            '${character.relationship.label(settings.locale)} • ${character.personality.label(settings.locale)}',
            style: TextStyle(
              fontSize: 13,
              color: Theme.of(context).brightness == Brightness.dark
                  ? const Color(0xFFAA9EC4)
                  : const Color(0xFF7B6F8A),
            ),
          ),
          const SizedBox(height: 20),
          Text(
            settings.t('첫 인사를 건네보세요! 👋', 'Say hello! 👋'),
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
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      color: isDark ? const Color(0xFF3D1515) : const Color(0xFFFFF3F3),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: Colors.red, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                fontSize: 13,
                color: isDark ? const Color(0xFFFF8080) : Colors.red,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          IconButton(
            icon: Icon(Icons.close, size: 18,
                color: isDark ? const Color(0xFFFF8080) : Colors.red),
            padding: EdgeInsets.zero,
            onPressed: onDismiss,
          ),
        ],
      ),
    );
  }
}
