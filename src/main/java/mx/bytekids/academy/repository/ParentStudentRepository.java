package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.ParentStudent;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, UUID> {
    List<ParentStudent> findByParent(User parent);
    List<ParentStudent> findByStudent(User student);
    Optional<ParentStudent> findByParentAndStudent(User parent, User student);

    @Query("SELECT ps.student FROM ParentStudent ps WHERE ps.parent = :parent")
    List<User> findChildrenByParent(User parent);
}
