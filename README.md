# ForjaNPCs

Plugin propio de NPCs para Spigot/Paper 1.21.1. Código 100% original (no usa ni una
línea de Citizens ni de ningún otro plugin de NPCs).

## Estado actual (Fase 1)

- NPCs basados en entidades reales de Bukkit (Villager, Zombie, cualquier `EntityType` con vida).
- Movimiento propio sin NMS: patrulla por waypoints, wander aleatorio, o combinación de ambos.
- Gancho best-effort con BetonQuest (dispara una conversación al hacer click en el NPC).
- Funciona igual en Spigot puro y en Paper (compila contra Paper-API, que es superset de Spigot).

## Pendiente (Fase 2)

- NPCs con skin de jugador real (requiere paquetes/protocolo — se recomienda apoyarse en
  ProtocolLib o packetevents como dependencia para que funcione en todas las versiones
  sin escribir código NMS por versión).

## Comandos

```
/forjanpc create <tipo_entidad> <nombre...>   Crea un NPC en tu posición y lo selecciona
/forjanpc remove [id]                         Elimina el NPC (o el seleccionado)
/forjanpc list                                Lista todos los NPCs
/forjanpc select <id>                         Selecciona un NPC para editarlo
/forjanpc waypoint add                        Agrega tu posición actual como waypoint
/forjanpc waypoint clear                      Borra todos los waypoints del NPC seleccionado
/forjanpc mode <none|patrol|wander|combo>     Define el modo de movimiento
/forjanpc conversation <id>                   Asocia una conversación de BetonQuest al NPC
```

## BetonQuest

BetonQuest no tiene una API pública genérica para que plugins de NPC de terceros se
registren como proveedor (solo reconoce a Citizens, FancyNpcs, ZNPCsPlus y MythicMobs).
Por eso, al interactuar con un NPC de ForjaNPCs, el plugin ejecuta directamente el comando
que arranca la conversación (ver `BetonQuestBridge.java`). Si tu versión de BetonQuest usa
otro comando, ajustá la constante `COMMAND_TEMPLATE` en esa clase.

## Compilar

Con GitHub Actions (recomendado, no necesitás nada instalado): subí este repo a GitHub,
el workflow en `.github/workflows/build.yml` compila solo y te deja el `.jar` en la pestaña
Actions → artifacts.

Local: `mvn clean package` (necesitás JDK 21 y Maven).
