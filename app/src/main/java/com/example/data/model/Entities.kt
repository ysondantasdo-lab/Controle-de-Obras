package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fornecedores")
data class Fornecedor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val telefone: String,
    val observacoes: String
)

@Entity(tableName = "prestadores")
data class Prestador(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val telefone: String,
    val observacoes: String
)

@Entity(tableName = "obras")
data class Obra(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val endereco: String
)

@Entity(tableName = "fornecedores_obra")
data class FornecedorObra(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val obraId: Int,
    val fornecedorId: Int,
    val valorServico: Double,
    val valorPago: Double,
    val dataPagamento: String
)

@Entity(tableName = "pagamentos_fornecedor")
data class PagamentoFornecedor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fornecedorObraId: Int,
    val valor: Double,
    val data: String
)

@Entity(tableName = "prestadores_obra")
data class PrestadorObra(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val obraId: Int,
    val prestadorId: Int,
    val valorMaterial: Double,
    val valorPago: Double,
    val dataPagamento: String
)

@Entity(tableName = "pagamentos_prestador")
data class PagamentoPrestador(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prestadorObraId: Int,
    val valor: Double,
    val data: String
)

@Entity(tableName = "lotes_obra")
data class LoteObra(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val obraId: Int,
    val nome: String,
    val tamanhoM2: Int,
    val valor: Double
)

@Entity(tableName = "burocracia_obra")
data class BurocraciaObra(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val obraId: Int,
    val nome: String,
    val valorPago: Double,
    val dataControle: String
)

// Helper DTOs to make JOIN operations painless
data class FornecedorObraWithDetails(
    val id: Int,
    val obraId: Int,
    val fornecedorId: Int,
    val valorServico: Double,
    val valorPago: Double,
    val dataPagamento: String,
    val fornecedorNome: String,
    val fornecedorTelefone: String
)

data class PrestadorObraWithDetails(
    val id: Int,
    val obraId: Int,
    val prestadorId: Int,
    val valorMaterial: Double,
    val valorPago: Double,
    val dataPagamento: String,
    val prestadorNome: String,
    val prestadorTelefone: String
)
