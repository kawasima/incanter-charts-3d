# 3D chart support for Incanter

This module adds 3D chart view to [Incanter](https://github.com/liebke/incanter).
Currently, it can draw 3D pie charts only.

This module has already been deprecated. If you are a decent engineer, you won't use 3D pie charts.

## Usage

Add this to dependencies in your project.clj.

```clojure
[net.unit8/incanter-charts-3d "1.5.4"]
```

And, write the following in your code.

```clojure
(use [incanter core charts-3d]

(view (pie-chart-3d (sel ds :cols :label)
                    (sel ds :cols :count)))
```


