import print, PV

spot = 100
spread = 0.005
while spread<= 0.01:
    up = spot + spread
    down = spot - spread
    if spread > 0.008:
        delta = (PV(up) - PV(down))/(spread*2)
        print(delta)
    spread = spread + 0.001