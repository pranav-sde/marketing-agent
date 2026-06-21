package com.marketingagent.repository;

import com.marketingagent.domain.magazine.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<Story, UUID> {
    List<Story> findByMagazine_Id(UUID magazineId);
    void deleteByMagazine_Id(UUID magazineId);
}
