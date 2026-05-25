package com.topik.topikai.service;

import com.topik.topikai.entity.HanjaPackUnlock;
import com.topik.topikai.repository.HanjaPackUnlockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HanjaPackService {

    /** SKU slug → packId trong hanja-bank.json */
    public static final Map<String, String> SKU_TO_PACK = Map.of(
            "KIIP", "kiip-hanja",
            "KIIP-HANJA", "kiip-hanja"
    );

    public static final int KIIP_PACK_PRICE = 99000;

    @Autowired
    private HanjaPackUnlockRepository hanjaPackUnlockRepository;

    public List<String> getUnlockedPackIds(Long userId) {
        return hanjaPackUnlockRepository.findByUserId(userId).stream()
                .map(HanjaPackUnlock::getPackId)
                .collect(Collectors.toList());
    }

    public boolean isPackUnlocked(Long userId, String packId) {
        return hanjaPackUnlockRepository.existsByUserIdAndPackId(userId, packId);
    }

    @Transactional
    public HanjaPackUnlock unlockPack(Long userId, String packId) {
        return hanjaPackUnlockRepository.findByUserIdAndPackId(userId, packId)
                .orElseGet(() -> {
                    HanjaPackUnlock unlock = new HanjaPackUnlock();
                    unlock.setUserId(userId);
                    unlock.setPackId(packId);
                    return hanjaPackUnlockRepository.save(unlock);
                });
    }

    public String resolvePackIdFromSku(String skuToken) {
        if (skuToken == null || skuToken.isBlank()) return null;
        return SKU_TO_PACK.get(skuToken.trim().toUpperCase());
    }
}
