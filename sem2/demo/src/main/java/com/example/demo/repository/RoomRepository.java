    package com.example.demo.repository;

    import com.example.demo.model.Room;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import java.math.BigDecimal;
    import java.util.List;
    import java.util.Optional; // Добавить импорт

    public interface RoomRepository extends JpaRepository<Room, Long> {
        List<Room> findByAvailableTrue();
        List<Room> findByNumberContainingIgnoreCase(String number);
        List<Room> findByType(String type);
        List<Room> findByCapacityGreaterThanEqual(Integer capacity);
        List<Room> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
        
        @Query("SELECT r FROM Room r WHERE r.available = true AND " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:minCapacity IS NULL OR r.capacity >= :minCapacity) AND " +
            "(:maxPrice IS NULL OR r.price <= :maxPrice)")
        List<Room> findAvailableRooms(@Param("type") String type,
                                    @Param("minCapacity") Integer minCapacity,
                                    @Param("maxPrice") BigDecimal maxPrice);
        
        Optional<Room> findByNumber(String number); // Уже правильно
        
        long countByAvailableTrue();
    }