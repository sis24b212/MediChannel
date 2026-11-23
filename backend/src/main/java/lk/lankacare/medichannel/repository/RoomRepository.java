package lk.lankacare.medichannel.repository;

import lk.lankacare.medichannel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByActiveTrueOrderByRoomNumberAsc();
    boolean existsByRoomNumberIgnoreCase(String roomNumber);
}
