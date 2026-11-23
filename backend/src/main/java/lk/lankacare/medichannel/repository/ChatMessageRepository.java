package lk.lankacare.medichannel.repository;

import lk.lankacare.medichannel.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConsultationIdOrderBySentAtAsc(Long consultationId);
}
