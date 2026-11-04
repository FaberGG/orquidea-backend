package com.orquidea.api.mapper;

import com.orquidea.api.dto.TrailSegmentDto;
import com.orquidea.api.model.TrailSegment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrailSegmentMapper {

    TrailSegmentDto aDto(TrailSegment tramo);
}
