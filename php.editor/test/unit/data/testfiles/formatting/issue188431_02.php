<?php
        function onEachSelectorShort($sw) {
            switch ($sw) {
                case "edit":
                    $foo = $this->BuildLink([
                        '_explode' => "*",
                    ]);
                    break;
            }
        }
?>