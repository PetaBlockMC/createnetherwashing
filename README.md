# Create: Nether Washing

A [Create](https://github.com/Creators-of-Create/Create) addon for **NeoForge 1.21.1 / Create 6.x** that lets you bulk-wash without placeable water — designed for the Nether, works anywhere.

## The mechanic

Pump water (from a tank, hose pulley, …) into a **Basin**. Place an **Encased Fan** 1–3 blocks above it, **facing down**, with its rotation reversed so it *pulls* air up through the basin. Items floating in the column between basin and fan — or sitting on a **belt or depot** inside it — are washed using the normal fan-washing (splashing) recipes. Every overworld washing recipe works, including modded ones.

```
        ║  (shaft)
      [Fan]   ← facing down, airflow reversed (pulling up)
        ↑
        ↑     ← items wash here (air, belt, or depot)
     [Basin]  ← holds water, gets drained
```

### Distance & speed

| Gap (blocks between basin and fan) | Min. fan speed | Washing time |
|---|---|---|
| 1 | 64 RPM  | 100% |
| 2 | 128 RPM | 150% |
| 3 | 192 RPM | 200% |
| 4+ | — | doesn't wash |

### Water consumption

An active column (fan spinning fast enough above a watered basin) **always consumes water**, items or not:
**100 mB/s at 64 RPM**, +25% per additional 64 RPM (scaling smoothly): 125 mB/s @128, 150 mB/s @192, 175 mB/s @256.
Set `drainRequiresItems = true` in the server config if you'd rather only pay water while items are actually washing.

### Blue ice tube

Enclose every gap block horizontally with **3 blue ice + 1 solid closing block** (a "tube") and the column washes at **full speed at any distance**, with water consumption **divided by 5** (20 mB/s @64 RPM = 1/50 bucket per second). The RPM requirement per block still applies — long columns stay stress-expensive.

All numbers are configurable (server config `createnetherwashing-server.toml`).

## Notes & quirks

- The fan must be *pulling* (airflow toward the fan, i.e. up). A fan blowing down at the basin does nothing.
- Items in the column get pulled up against the fan — belts/depots are the tidy way to automate.
- A belt inside the column blocks airflow below itself, so items *underneath* it won't wash.
- Any fluid tagged `create:fan_processing_catalysts/splashing` works, not just water.

## Building

```
./gradlew build        # jar in build/libs/
./gradlew runClient    # dev client with Create
```
