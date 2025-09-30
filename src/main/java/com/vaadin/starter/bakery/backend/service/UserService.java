package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.UserRepository;

/**
 * Serviço responsável pela lógica de negócio relacionada à entidade {@link User}.
 * Fornece operações CRUD com restrições específicas como:
 * - Prevenção de modificação/remoção de usuários bloqueados;
 * - Proibição de autoexclusão;
 * - Filtros para busca textual.
 */
@Service
public class UserService implements FilterableCrudService<User> {

    /** Mensagem de erro usada ao tentar modificar ou excluir um usuário bloqueado. */
    public static final String MODIFY_LOCKED_USER_NOT_PERMITTED = "User has been locked and cannot be modified or deleted";

    /** Mensagem de erro usada ao tentar excluir a própria conta. */
    private static final String DELETING_SELF_NOT_PERMITTED = "You cannot delete your own account";

    private final UserRepository userRepository;

    /**
     * Construtor que injeta o {@link UserRepository}.
     *
     * @param userRepository repositório de usuários
     */
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Busca usuários com base em um filtro textual opcional.
     * O filtro é aplicado aos campos: email, nome, sobrenome e papel (role).
     *
     * @param filter   filtro opcional de texto
     * @param pageable informações de paginação
     * @return página de usuários que correspondem ao filtro
     */
    public Page<User> findAnyMatching(Optional<String> filter, Pageable pageable) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return getRepository()
                    .findByEmailLikeIgnoreCaseOrFirstNameLikeIgnoreCaseOrLastNameLikeIgnoreCaseOrRoleLikeIgnoreCase(
                            repositoryFilter, repositoryFilter, repositoryFilter, repositoryFilter, pageable);
        } else {
            return find(pageable);
        }
    }

    /**
     * Conta o número de usuários que correspondem ao filtro textual opcional.
     *
     * @param filter filtro opcional de texto
     * @return quantidade de usuários que correspondem ao filtro
     */
    @Override
    public long countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return userRepository.countByEmailLikeIgnoreCaseOrFirstNameLikeIgnoreCaseOrLastNameLikeIgnoreCaseOrRoleLikeIgnoreCase(
                    repositoryFilter, repositoryFilter, repositoryFilter, repositoryFilter);
        } else {
            return count();
        }
    }

    /**
     * Retorna o repositório utilizado por este serviço.
     *
     * @return repositório de usuários
     */
    @Override
    public UserRepository getRepository() {
        return userRepository;
    }

    /**
     * Retorna uma página de usuários sem aplicar filtros.
     *
     * @param pageable informações de paginação
     * @return página de usuários
     */
    public Page<User> find(Pageable pageable) {
        return getRepository().findBy(pageable);
    }

    /**
     * Salva um usuário no banco de dados.
     * Lança exceção se o usuário estiver bloqueado.
     *
     * @param currentUser o usuário logado atualmente
     * @param entity      a entidade de usuário a ser salva
     * @return o usuário salvo
     * @throws UserFriendlyDataException se o usuário estiver bloqueado
     */
    @Override
    public User save(User currentUser, User entity) {
        throwIfUserLocked(entity);
        return getRepository().saveAndFlush(entity);
    }

    /**
     * Exclui um usuário, respeitando regras como:
     * - Não permitir exclusão do próprio usuário;
     * - Não permitir exclusão de usuários bloqueados.
     *
     * @param currentUser   o usuário logado atualmente
     * @param userToDelete  o usuário a ser excluído
     * @throws UserFriendlyDataException se alguma regra de negócio for violada
     */
    @Override
    @Transactional
    public void delete(User currentUser, User userToDelete) {
        throwIfDeletingSelf(currentUser, userToDelete);
        throwIfUserLocked(userToDelete);
        FilterableCrudService.super.delete(currentUser, userToDelete);
    }

    /**
     * Lança exceção se o usuário a ser excluído for o próprio usuário atual.
     *
     * @param currentUser usuário atual logado
     * @param user        usuário alvo da exclusão
     */
    private void throwIfDeletingSelf(User currentUser, User user) {
        if (currentUser.equals(user)) {
            throw new UserFriendlyDataException(DELETING_SELF_NOT_PERMITTED);
        }
    }

    /**
     * Lança exceção se o usuário estiver bloqueado.
     *
     * @param entity usuário a ser validado
     */
    private void throwIfUserLocked(User entity) {
        if (entity != null && entity.isLocked()) {
            throw new UserFriendlyDataException(MODIFY_LOCKED_USER_NOT_PERMITTED);
        }
    }

    /**
     * Cria uma nova instância vazia de {@link User}.
     *
     * @param currentUser o usuário logado atualmente (não utilizado neste caso)
     * @return nova instância de usuário
     */
    @Override
    public User createNew(User currentUser) {
        return new User();
    }
}