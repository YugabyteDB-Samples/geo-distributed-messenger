package com.yugabyte.app.messenger.data.service;

import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.repository.ProfileRepository;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository repository;

    @Autowired
    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    public Optional<Profile> get(GeoId id) {
        return repository.findById(id);
    }

    public Profile update(Profile entity) {
        return repository.save(entity);
    }

    public void delete(GeoId id) {
        repository.deleteById(id);
    }

    public Page<Profile> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
