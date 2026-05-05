package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public Subject findById(UUID id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia", id));
    }

    public List<Subject> findAll() {
        return subjectRepository.findByIsActiveTrueOrderByName();
    }

    @Transactional
    public Subject create(Subject subject) {
        if (subjectRepository.existsByName(subject.getName())) {
            throw new BusinessException("La materia '" + subject.getName() + "' ya existe");
        }
        if (subject.getIsActive() == null) subject.setIsActive(true);
        return subjectRepository.save(subject);
    }

    @Transactional
    public Subject update(UUID id, Subject updated) {
        Subject subject = findById(id);
        subject.setName(updated.getName());
        subject.setIcon(updated.getIcon());
        subject.setColor(updated.getColor());
        subject.setDescription(updated.getDescription());
        return subjectRepository.save(subject);
    }

    @Transactional
    public void deactivate(UUID id) {
        Subject subject = findById(id);
        subject.setIsActive(false);
        subjectRepository.save(subject);
    }
}
