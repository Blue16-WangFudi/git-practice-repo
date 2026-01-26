import flet as ft
try:
    import flet.colors as colors_module
    print("Imported flet.colors successfully")
    print("RED in colors module:", colors_module.RED)
except ImportError as e:
    print("Could not import flet.colors:", e)

if hasattr(ft, 'Colors'):
    print("ft.Colors exists")
    print("ft.Colors attributes:", dir(ft.Colors)[:5])
