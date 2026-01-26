import flet as ft
try:
    import flet.icons as icons_module
    print("Imported flet.icons successfully")
except ImportError as e:
    print("Could not import flet.icons:", e)

if hasattr(ft, 'Icons'):
    print("ft.Icons exists")
    print("ft.Icons attributes:", dir(ft.Icons)[:5])
else:
    print("ft.Icons does not exist")
