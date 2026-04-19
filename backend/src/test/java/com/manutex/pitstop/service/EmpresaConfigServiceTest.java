package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.EmpresaConfig;
import com.manutex.pitstop.domain.repository.EmpresaConfigRepository;
import com.manutex.pitstop.web.dto.EmpresaConfigRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmpresaConfigServiceTest {

    @Mock EmpresaConfigRepository repository;
    @Mock StorageService storageService;

    @InjectMocks EmpresaConfigService service;

    @Test
    void deveRetornarConfigPadraoCasoNaoExista() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        var response = service.buscar();

        assertThat(response.nome()).isEqualTo("Minha Oficina");
        assertThat(response.logoUrl()).isNull();
    }

    @Test
    void deveSalvarConfigDaEmpresa() {
        EmpresaConfigRequest request = new EmpresaConfigRequest(
            "Auto Center Pitstop", "12.345.678/0001-99",
            "Rua das Flores, 123", "(11) 3333-4444", "contato@pitstop.com"
        );

        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.salvar(request);

        assertThat(response.nome()).isEqualTo("Auto Center Pitstop");
        assertThat(response.cnpj()).isEqualTo("12.345.678/0001-99");
        assertThat(response.email()).isEqualTo("contato@pitstop.com");
    }

    @Test
    void deveSalvarConfigAtualizandoExistente() {
        EmpresaConfig existing = EmpresaConfig.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .nome("Nome Antigo").build();

        when(repository.findById(any())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmpresaConfigRequest request = new EmpresaConfigRequest("Nome Novo", null, null, null, null);
        var response = service.salvar(request);

        assertThat(response.nome()).isEqualTo("Nome Novo");
    }

    @Test
    void deveFazerUploadDeLogo() {
        MockMultipartFile logo = new MockMultipartFile(
            "file", "logo.png", "image/png", new byte[]{1, 2, 3, 4}
        );

        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.generatePresignedDownloadUrl(any())).thenReturn(null);

        var response = service.uploadLogo(logo);

        verify(storageService).storeImage(anyString(), any(), eq("image/png"));
        assertThat(response).isNotNull();
    }

    @Test
    void deveDeletarLogoAntigoAoFazerUpload() {
        EmpresaConfig existing = EmpresaConfig.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .nome("Oficina").logoKey("empresa/logo/old-key").logoMimeType("image/png").build();

        MockMultipartFile logo = new MockMultipartFile(
            "file", "novo.png", "image/png", new byte[]{5, 6, 7, 8}
        );

        when(repository.findById(any())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.generatePresignedDownloadUrl(any())).thenReturn(null);

        service.uploadLogo(logo);

        verify(storageService).delete("empresa/logo/old-key");
        verify(storageService).storeImage(anyString(), any(), eq("image/png"));
    }

    @Test
    void deveRejeitarLogoVazio() {
        MockMultipartFile vazio = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadLogo(vazio))
            .isInstanceOf(EmpresaConfigService.LogoUploadException.class)
            .hasMessageContaining("vazio");
    }

    @Test
    void deveRejeitarLogoComFormatoInvalido() {
        MockMultipartFile pdf = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.uploadLogo(pdf))
            .isInstanceOf(EmpresaConfigService.LogoUploadException.class)
            .hasMessageContaining("Formato");
    }

    @Test
    void deveRejeitarLogoMaiorQue2MB() {
        byte[] grande = new byte[3 * 1024 * 1024];
        MockMultipartFile logo = new MockMultipartFile("file", "grande.png", "image/png", grande);

        assertThatThrownBy(() -> service.uploadLogo(logo))
            .isInstanceOf(EmpresaConfigService.LogoUploadException.class)
            .hasMessageContaining("2 MB");
    }
}
