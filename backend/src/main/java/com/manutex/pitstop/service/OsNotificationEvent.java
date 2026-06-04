package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.enums.NotificationEvent;

import java.util.UUID;

/**
 * Evento de domínio publicado por ManutencaoService para desacoplar
 * a lógica de negócio do sistema de notificações.
 *
 * @param empresaId      tenant da OS
 * @param manutencaoId   ID da OS
 * @param clienteId      ID do cliente (para LGPD)
 * @param clienteTelefone telefone do cliente (para WhatsApp)
 * @param clienteEmail    e-mail do cliente (para e-mail)
 * @param clienteNome     nome do cliente
 * @param veiculoPlaca    placa do veículo
 * @param veiculoModelo   modelo do veículo
 * @param rastreioLink    link público de rastreio
 * @param evento          tipo de evento que disparou a notificação
 */
public record OsNotificationEvent(
    UUID empresaId,
    UUID manutencaoId,
    UUID clienteId,
    String clienteTelefone,
    String clienteEmail,
    String clienteNome,
    String veiculoPlaca,
    String veiculoModelo,
    String rastreioLink,
    NotificationEvent evento
) {}
