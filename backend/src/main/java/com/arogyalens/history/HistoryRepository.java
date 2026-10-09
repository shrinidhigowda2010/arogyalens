package com.arogyalens.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Data access for {@link HistoryEntry}; every query is scoped to a device id. */
public interface HistoryRepository extends JpaRepository<HistoryEntry, Long> {

    List<HistoryEntry> findByDeviceIdOrderByCreatedAtDescIdDesc(String deviceId, Pageable page);

    @Transactional
    long deleteByDeviceId(String deviceId);

    @Transactional
    long deleteByIdAndDeviceId(Long id, String deviceId);
}
