package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.PageResult;
import com.hospital.sistemamedico.model.dto.UserForm;
import com.hospital.sistemamedico.model.entity.*;
import com.hospital.sistemamedico.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** CU-01 Mantenimiento de Usuarios. */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final Pattern ALNUM = Pattern.compile("^[A-Za-z0-9]+$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern DIGITS13 = Pattern.compile("^\\d{13}$");
    private static final Pattern DIGITS8 = Pattern.compile("^\\d{8}$");

    private final InternalUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // ---------------------------------------------------------------- listado

    @Transactional(readOnly = true)
    public PageResult<InternalUser> search(String field, String text, int page, int size) {
        String q = text == null ? "" : text.trim();
        if (q.length() > 25) {
            throw new BusinessRuleException("El campo de búsqueda no puede exceder los 25 caracteres.");
        }
        int safeSize = (size == 10 || size == 25 || size == 50) ? size : 10;
        List<InternalUser> all = userRepository.findAllNotDeleted();
        if (!q.isEmpty()) {
            String needle = normalize(q);
            all = all.stream().filter(u -> normalize(valueOf(u, field)).contains(needle)).toList();
        }
        return PageResult.of(all, page, safeSize);
    }

    private String valueOf(InternalUser u, String field) {
        String f = field == null ? "nombre" : field;
        return switch (f) {
            case "id" -> String.valueOf(u.getId());
            case "correo" -> u.getEmail() == null ? "" : u.getEmail();
            case "rol" -> u.getRole().getName();
            case "usuario" -> u.getUsername();
            case "dpi" -> u.getDpi() == null ? "" : u.getDpi();
            default -> u.getFullName();
        };
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- consulta

    @Transactional(readOnly = true)
    public UserForm loadForm(Integer id) {
        InternalUser u = find(id);
        UserForm f = new UserForm();
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setUsername(u.getUsername());
        f.setDpi(u.getDpi());
        f.setPhone(u.getPhone());
        f.setRoleId(u.getRole().getId());
        f.setNit(u.getNit());
        f.setInsuranceNumber(u.getInsuranceNumber());
        f.setBranchId(u.getBranch() == null ? null : u.getBranch().getId());
        f.setSpecialtyId(u.getSpecialty() == null ? null : u.getSpecialty().getId());
        f.setActive(u.isActive());
        return f;
    }

    public List<Role> activeRoles() {
        return roleRepository.findAll().stream().filter(Role::isActive).toList();
    }

    public List<Branch> activeBranches() {
        return branchRepository.findByActiveTrue();
    }

    public List<Specialty> activeSpecialties() {
        return specialtyRepository.findByActiveTrue();
    }

    // ---------------------------------------------------------------- validación (RN-CU01-03..14)

    /** Devuelve campo -> mensaje. Vacío si todo es válido. */
    @Transactional(readOnly = true)
    public Map<String, String> validate(UserForm f, Integer editingId) {
        Map<String, String> e = new LinkedHashMap<>();
        boolean creating = editingId == null;

        String name = trim(f.getFullName());
        if (name.isEmpty()) {
            e.put("fullName", "El campo Nombre es obligatorio.");
        } else if (name.length() < 10 || name.length() > 100) {
            e.put("fullName", "El nombre debe contener entre 10 y 100 caracteres. Usted ingresó " + name.length() + " caracteres.");
        }

        String email = trim(f.getEmail());
        if (email.isEmpty()) {
            e.put("email", "El correo electrónico es obligatorio.");
        } else if (email.length() > 150 || !EMAIL.matcher(email).matches()) {
            e.put("email", "Ingrese un correo electrónico válido.");
        } else if (creating ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, editingId)) {
            e.put("email", "El correo electrónico " + email + " ya se encuentra registrado.");
        }

        String username = trim(f.getUsername());
        if (username.isEmpty()) {
            e.put("username", "El campo Usuario es obligatorio.");
        } else if (username.length() > 9) {
            e.put("username", "El usuario no puede exceder los 9 caracteres.");
        } else if (username.length() < 8) {
            e.put("username", "El usuario debe contener al menos 8 caracteres.");
        } else if (!ALNUM.matcher(username).matches()) {
            e.put("username", "El usuario debe contener únicamente caracteres alfanuméricos.");
        } else {
            userRepository.findByUsername(username).ifPresent(other -> {
                if (creating || !other.getId().equals(editingId)) {
                    e.put("username", "El nombre de usuario " + username + " ya se encuentra registrado. Por favor, elija otro.");
                }
            });
        }

        String pwd = f.getPassword() == null ? "" : f.getPassword();
        if (creating && pwd.isEmpty()) {
            e.put("password", "La contraseña es obligatoria.");
        } else if (!pwd.isEmpty() && pwd.length() < 12) {
            e.put("password", "La contraseña debe contener al menos 12 caracteres.");
        }

        String dpi = trim(f.getDpi());
        if (!dpi.isEmpty()) {
            if (!DIGITS13.matcher(dpi).matches()) {
                e.put("dpi", "El DPI debe contener exactamente 13 dígitos numéricos.");
            } else if (creating ? userRepository.existsByDpi(dpi) : userRepository.existsByDpiAndIdNot(dpi, editingId)) {
                e.put("dpi", "El DPI ya se encuentra registrado.");
            }
        }

        String phone = trim(f.getPhone());
        if (!phone.isEmpty() && !DIGITS8.matcher(phone).matches()) {
            e.put("phone", "El teléfono debe contener exactamente 8 dígitos.");
        }

        String nit = trim(f.getNit());
        if (!nit.isEmpty() && (nit.length() < 8 || nit.length() > 9 || !ALNUM.matcher(nit).matches())) {
            e.put("nit", "El NIT debe contener entre 8 y 9 caracteres alfanuméricos.");
        }

        String ins = trim(f.getInsuranceNumber());
        if (!ins.isEmpty() && (ins.length() < 5 || ins.length() > 50)) {
            e.put("insuranceNumber", "El número de seguro debe contener entre 5 y 50 caracteres.");
        }

        Role role = null;
        if (f.getRoleId() == null) {
            e.put("roleId", "Debe seleccionar un rol para el usuario.");
        } else {
            role = roleRepository.findById(f.getRoleId()).orElse(null);
            if (role == null || !role.isActive()) {
                e.put("roleId", "Debe seleccionar un rol para el usuario.");
                role = null;
            }
        }

        if (creating && f.getBranchId() == null) {
            e.put("branchId", "Debe seleccionar una sucursal para el usuario.");
        }

        if (role != null && isDoctor(role) && f.getSpecialtyId() == null) {
            e.put("specialtyId", "Debe seleccionar una especialidad para el médico.");
        }
        return e;
    }

    // ---------------------------------------------------------------- escritura

    @Transactional
    public InternalUser create(UserForm f, Integer actorId) {
        InternalUser u = new InternalUser();
        apply(u, f);
        u.setUsername(trim(f.getUsername()));
        u.setPassword(passwordEncoder.encode(f.getPassword()));
        u = userRepository.save(u);
        auditService.log("CREAR", "USUARIO", u.getId(), "Usuario " + u.getUsername() + " (" + u.getRole().getName() + ")", actorId);
        return u;
    }

    @Transactional
    public InternalUser update(Integer id, UserForm f, Integer actorId) {
        InternalUser u = find(id);
        apply(u, f);
        u.setUsername(trim(f.getUsername()));
        if (f.getPassword() != null && !f.getPassword().isEmpty()) {
            u.setPassword(passwordEncoder.encode(f.getPassword()));
        }
        u = userRepository.save(u);
        auditService.log("ACTUALIZAR", "USUARIO", u.getId(), "Usuario " + u.getUsername(), actorId);
        return u;
    }

    @Transactional
    public String delete(Integer id, Integer actorId) {
        InternalUser u = find(id);
        if (u.getId().equals(actorId)) {
            throw new BusinessRuleException("No puede eliminar su propio usuario.");
        }
        u.setDeleted(true);
        u.setActive(false);
        userRepository.save(u);
        auditService.log("ELIMINAR", "USUARIO", u.getId(), "Usuario " + u.getUsername(), actorId);
        return u.getUsername();
    }

    // ---------------------------------------------------------------- auxiliares

    private void apply(InternalUser u, UserForm f) {
        Role role = roleRepository.findById(f.getRoleId())
                .orElseThrow(() -> new BusinessRuleException("Debe seleccionar un rol para el usuario."));
        u.setFullName(trim(f.getFullName()));
        u.setEmail(trim(f.getEmail()));
        u.setDpi(blankToNull(f.getDpi()));
        u.setPhone(blankToNull(f.getPhone()));
        u.setNit(blankToNull(f.getNit()));
        u.setInsuranceNumber(blankToNull(f.getInsuranceNumber()));
        u.setRole(role);
        u.setBranch(f.getBranchId() == null ? null : branchRepository.findById(f.getBranchId()).orElse(null));
        u.setSpecialty(isDoctor(role) && f.getSpecialtyId() != null
                ? specialtyRepository.findById(f.getSpecialtyId()).orElse(null) : null);
        u.setActive(f.isActive());
    }

    private InternalUser find(Integer id) {
        return userRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new BusinessRuleException("El usuario solicitado no existe."));
    }

    private static boolean isDoctor(Role r) {
        return "MEDICO".equals(normalize(r.getName()).toUpperCase(Locale.ROOT));
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankToNull(String s) {
        String t = trim(s);
        return t.isEmpty() ? null : t;
    }
}
