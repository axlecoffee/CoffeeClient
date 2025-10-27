# CoffeeClient

A custom addon for NotEnoughUpdates that works on Lunar Client.

## Features

- **Module System**: Toggleable modules with keybind support
- **Command System**: Dot-prefix commands for easy control
- **Config System**: JSON-based config save/load with multiple profiles
- **Event-Based**: Uses Forge events for compatibility

## Commands

### .coffee / .list / .l
Lists all available modules with their status (ON/OFF) and keybinds.

### .bind / .b
Bind modules to keys.
- `.bind <module> <key>` - Bind a module to a key (e.g., `.bind test o`)
- `.bind <module> none` - Unbind a module
- `.bind list` - List all current keybinds
- `.bind * <key>` - Bind all modules to the same key

### .toggle / .t
Toggle modules on/off.
- `.toggle <module>` - Toggle a module
- `.toggle <module> on` - Turn on a module
- `.toggle <module> off` - Turn off a module

### .config / .c / .cfg
Manage configs.
- `.config save <name>` - Save current config
- `.config save` - Save to default config
- `.config load <name>` - Load a config
- `.config list` - List all saved configs (click to load)
- `.config folder` - Open config folder

## Modules

### Test
A simple test module that sends "Hello World" to chat when toggled.

**Usage**: `.bind test o` then press 'O' to toggle

## Package Structure

```
io.github.moulberry.notenoughupdate.coffeeclient/
├── CoffeeClient.java           - Main initialization class
├── util/
│   ├── ChatUtil.java          - Chat formatting utilities
│   └── KeyBindUtil.java       - Keybind utilities
├── module/
│   ├── Module.java            - Base module class
│   ├── ModuleManager.java     - Module management
│   └── modules/
│       └── TestModule.java    - Test module
├── command/
│   ├── Command.java           - Base command class
│   ├── CommandManager.java    - Command interception & routing
│   └── commands/
│       ├── CoffeeCommand.java - .coffee command
│       ├── BindCommand.java   - .bind command
│       ├── ToggleCommand.java - .toggle command
│       └── ConfigCommand.java - .config command
└── config/
    └── Config.java            - JSON config save/load
```

## Technical Details

### Lunar Client Compatibility
- All code is in `io.github.moulberry.notenoughupdates` package (required for net.minecraft access)
- Uses Forge events instead of mixins (limited mixin support on Lunar)
- No external dependencies - uses NEU's existing dependencies
- Event-based architecture for maximum compatibility

### How It Works
1. **Initialization**: `CoffeeClient.init()` is called from `NotEnoughUpdates.preinit()`
2. **Command Interception**: `CommandManager` listens for `GuiScreenEvent.KeyboardInputEvent.Pre` to intercept chat messages starting with "."
3. **Keybind Handling**: `ModuleManager` listens for `InputEvent.KeyInputEvent` to handle module keybinds
4. **Config Storage**: Configs are saved to `./config/CoffeeClient/` as JSON files

## Adding New Modules

1. Create a new class extending `Module` in the `modules` package
2. Override `onEnabled()` and `onDisabled()` as needed
3. Register the module in `CoffeeClient.registerModules()`

Example:
```java
public class MyModule extends Module {
    public MyModule() {
        super("MyModule", false);
    }
    
    @Override
    public void onEnabled() {
        ChatUtil.sendMessage("&aModule enabled!");
    }
    
    @Override
    public void onDisabled() {
        ChatUtil.sendMessage("&cModule disabled!");
    }
}
```

## Credits

Based on the module/command system from [OpenMyau](https://github.com/sakthic01/OpenMyau) by sakthic01.

## License

Part of NotEnoughUpdates - See COPYING and COPYING.LESSER for license information.
