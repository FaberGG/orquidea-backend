package com.orquidea.api.repository;

import com.orquidea.api.model.DeletedTaxon;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Solo lectura: los registros los crea un trigger de la base de datos. */
public interface DeletedTaxonRepository extends Repository<DeletedTaxon, UUID> {

    /** Ids eliminados desde esa fecha, sin los que volvieron a existir (por ejemplo, al restaurar un respaldo). */
    @Query("""
            select d.id from DeletedTaxon d
            where d.fechaEliminacion >= :desde
              and not exists (select 1 from Taxon t where t.id = d.id)
            order by d.fechaEliminacion, d.id
            """)
    List<UUID> idsEliminadosDesde(@Param("desde") Instant desde);
}
