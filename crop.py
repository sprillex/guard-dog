from PIL import Image
import os

img = Image.open("/tmp/file_attachments/Guard Dog icons.jpeg")
width, height = img.size
print(f"Image size: {width}x{height}")

# Central main squircle
left_main = (1024 - 400) // 2
right_main = left_main + 400
top_main = 140
bottom_main = top_main + 400

main_icon = img.crop((left_main, top_main, right_main, bottom_main))

# Create dirs if missing
os.makedirs("app/src/main/res/mipmap-xxxhdpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-xxhdpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-xhdpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-hdpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-mdpi", exist_ok=True)

main_icon.resize((192, 192)).save("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
main_icon.resize((144, 144)).save("app/src/main/res/mipmap-xxhdpi/ic_launcher.png")
main_icon.resize((96, 96)).save("app/src/main/res/mipmap-xhdpi/ic_launcher.png")
main_icon.resize((72, 72)).save("app/src/main/res/mipmap-hdpi/ic_launcher.png")
main_icon.resize((48, 48)).save("app/src/main/res/mipmap-mdpi/ic_launcher.png")

# Using the main icon as the round one as well, since it looks like a shield squircle and adaptive icons handle shapes.
main_icon.resize((192, 192)).save("app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png")
main_icon.resize((144, 144)).save("app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png")
main_icon.resize((96, 96)).save("app/src/main/res/mipmap-xhdpi/ic_launcher_round.png")
main_icon.resize((72, 72)).save("app/src/main/res/mipmap-hdpi/ic_launcher_round.png")
main_icon.resize((48, 48)).save("app/src/main/res/mipmap-mdpi/ic_launcher_round.png")
