# Scripts de développement disponibles

## Commandes rapides

### Linux/Mac

```bash
./server.sh         # Démarre le serveur
./server.sh stop     # Arrête le serveur
./server.sh restart  # Redémarre le serveur
./server.sh status   # Vérifie le statut
```

### Windows

```batch
server.bat           # Démarre le serveur
server.bat stop      # Arrête le serveur
server.bat restart   # Redémarre le serveur
server.bat status    # Vérifie le statut
```

### Universel

```bash
./server             # Détecte automatiquement l'OS
```

## Fonctionnalités

✅ **Rechargement automatique** - DevTools activé  
✅ **Commandes simples** - Plus besoin de taper `mvn clean spring-boot:run`  
✅ **Multi-plateforme** - Linux, Mac, Windows  
✅ **Gestion des processus** - Start/stop/restart/status

## URL du serveur

🔗 http://localhost:8080

## Note

Les changements de code déclenchent un redémarrage automatique du serveur grâce à Spring Boot DevTools.
