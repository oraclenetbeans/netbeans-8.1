<?php
        function onEachSelector($sw) {
            switch ($sw) {
                case "edit":
                    $foo = $this->BuildLink(array(
                        '_explode' => "*",
                    ));
                    break;
            }
        }
?>