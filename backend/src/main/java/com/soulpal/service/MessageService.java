package com.soulpal.service;

import com.soulpal.model.Message;
import com.soulpal.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public List<Message> getMessages(String characterId, int page, int size) {
        List<Message> messages = messageRepository.findByCharacterIdOrderByCreatedAtDesc(
                characterId, PageRequest.of(page, size));
        Collections.reverse(messages);
        return messages;
    }

    public List<Message> searchMessages(String characterId, String keyword) {
        return messageRepository.searchByContent(characterId, keyword);
    }

    public List<Message> getRecentForContext(String characterId, int count) {
        List<Message> messages = messageRepository.findByCharacterIdOrderByCreatedAtDesc(
                characterId, PageRequest.of(0, count));
        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public Message save(String characterId, String content, boolean isUser) {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .characterId(characterId)
                .content(content)
                .user(isUser)
                .createdAt(LocalDateTime.now())
                .build();
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteLastAiMessage(String characterId) {
        List<Message> messages = messageRepository.findByCharacterIdOrderByCreatedAtDesc(
                characterId, PageRequest.of(0, 1));
        if (!messages.isEmpty() && !messages.get(0).isUser()) {
            messageRepository.delete(messages.get(0));
        }
    }

    @Transactional
    public void clearAll(String characterId) {
        messageRepository.deleteByCharacterId(characterId);
    }

    public long countMessages(String characterId) {
        return messageRepository.countByCharacterId(characterId);
    }
}
