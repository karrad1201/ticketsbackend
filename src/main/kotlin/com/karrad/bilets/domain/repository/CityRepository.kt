package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.City
import org.springframework.cache.annotation.Cacheable

interface CityRepository {
    @Cacheable("cities.all")
    fun findAll(): List<City>
}
