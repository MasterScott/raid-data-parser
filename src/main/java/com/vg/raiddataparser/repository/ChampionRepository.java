package com.vg.raiddataparser.repository;

import com.vg.raiddataparser.model.Champion;
import org.springframework.data.repository.CrudRepository;

public interface ChampionRepository extends CrudRepository<Champion, Integer> {
}