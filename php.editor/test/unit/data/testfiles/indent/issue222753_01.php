<?php

class A {

    public function getDataSource(Search $search = null) {
        $fluent = $this->getBaseQuery()->omg();
        return $fluent^->toDatoDataSource();
    }

}

?>