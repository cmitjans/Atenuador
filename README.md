# Atenuador

App de Android que oscurece la pantalla por debajo del brillo mínimo, sobre todas las apps.

## Descargar

La versión más reciente está siempre en **Releases → Atenuador.apk**:
https://github.com/cmitjans/Atenuador/releases/latest

Ábrelo desde el celular e instálalo. Android te pedirá permitir instalar apps de este origen; es normal en apps que no vienen de Google Play.

## Cómo se usa

- Activa **Atenuar pantalla** y ajusta la intensidad (hasta 80 %, el máximo que permite Android para que siempre puedas ver y tocar la pantalla).
- Agrega el botón **Atenuador** a los ajustes rápidos para encenderlo con un toque.
- Mientras está activo, la notificación tiene botones para aclarar, oscurecer o apagar.
- Si se apaga solo: Ajustes → Apps → Atenuador → Ahorro de batería → **Sin restricciones**.

## Permisos

Esta es la lista completa (ver `app/AndroidManifest.xml`):

| Permiso | Para qué |
| --- | --- |
| Mostrar sobre otras apps | Dibujar la capa oscura. Es el único que apruebas a mano. |
| Servicio en primer plano | Mantener la capa activa mientras usas otras apps. |
| Notificaciones | El aviso "Pantalla atenuada" con sus botones. |

**No tiene permiso de internet**: Android no le deja conectarse a nada.

## Cómo funciona

Una capa negra semitransparente cubre la pantalla. La capa no recibe toques, así que todo lo que tocas llega a la app de abajo. El código está en `app/src`.

## Compilar

`build.sh` compila con las herramientas del SDK de Android, sin Gradle. GitHub lo ejecuta automáticamente con cada cambio (`.github/workflows/compilar.yml`) y publica el APK en Releases.

La clave de firma (`firma/atenuador.jks`) está en el repositorio para que cada versión se pueda instalar encima de la anterior. Como el repositorio es público, cualquiera podría firmar un APK con ella; por eso, instala solo los APK que descargues de este repositorio.
