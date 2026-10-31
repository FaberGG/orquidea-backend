package com.orquidea.api.repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.orquidea.api.model.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findAllByOrderByFechaCreacionDesc();

      @Query("""
      select a from Announcement a
      where a.estado = com.orquidea.api.model.Estado.ACTIVO
        and (a.fechaExpiracion is null or a.fechaExpiracion > :ahora)
      order by a.fechaCreacion desc
      """)
    List<Announcement> findVisibles(@Param("ahora") Instant ahora);

}
