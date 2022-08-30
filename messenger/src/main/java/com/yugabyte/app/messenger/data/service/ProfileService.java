package com.yugabyte.app.messenger.data.service;

import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.repository.ProfileRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository repository;

    private final HashMap<GeoId, Profile> localCache;

    @Autowired
    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
        localCache = new HashMap<>();
    }

    @Bean
    public CommandLineRunner preloadProfiles() {
        return args -> {
            List<Profile> users = repository.findAll();
            users.forEach(user -> localCache.put(new GeoId(user.getId(), user.getCountryCode()), user));
            System.out.println("Preloaded all Profiles to local cache");
        };
    }

    public Optional<Profile> get(GeoId id) {
        Profile user = localCache.get(id);

        if (user != null)
            return Optional.of(user);

        Optional<Profile> dbUser = repository.findById(id);

        if (dbUser.isPresent())
            localCache.put(id, dbUser.get());

        return dbUser;
    }

    public Profile update(Profile entity) {
        if (entity.getId() != 0) {
            GeoId geoId = new GeoId();
            geoId.setId(entity.getId());
            geoId.setCountryCode(entity.getCountryCode());

            localCache.put(geoId, entity);
        }

        return repository.save(entity);
    }

    public void delete(GeoId id) {
        localCache.remove(id);
        repository.deleteById(id);
    }

    public Page<Profile> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
